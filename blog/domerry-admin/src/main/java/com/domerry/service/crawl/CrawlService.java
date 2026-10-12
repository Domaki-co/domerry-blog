package com.domerry.service.crawl;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.domerry.entity.SysConfig;
import com.domerry.exception.ServiceException;
import com.domerry.service.SysArticleService;
import com.domerry.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 内容来源拉取服务：读取 sys_config 的 crawl.sources（JSON 数组），
 * 支持 rss / html 配方 / discourse（linux.do 等 Discourse 论坛）三类来源，
 * 抓到的内容统一走 saveImportedArticle 管道（去重、消毒、默认私密、署名）。
 * 定时批量入口见 {@link CrawlTask}，手动单篇见 SysArticleService#reptile。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlService {

    private static final String CONFIG_KEY = "crawl.sources";

    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private static final int MAX_ITEMS_CAP = 20;

    private final SysConfigService sysConfigService;

    private final SysArticleService sysArticleService;

    /**
     * 拉取全部启用的来源，返回新增文章数；单个来源失败不影响其他来源
     */
    public int pullAll() {
        SysConfig config = sysConfigService.selectConfigByKey(CONFIG_KEY);
        if (config == null || StringUtils.isBlank(config.getConfigValue())) {
            log.info("未配置抓取来源（sys_config: {}），跳过", CONFIG_KEY);
            return 0;
        }
        List<CrawlSource> sources;
        try {
            sources = JSONArray.parseArray(config.getConfigValue(), CrawlSource.class);
        } catch (Exception e) {
            log.error("抓取来源配置解析失败，请检查 sys_config[{}] 的 JSON 格式", CONFIG_KEY, e);
            return 0;
        }
        int total = 0;
        for (CrawlSource source : sources) {
            if (source.getEnabled() != null && !source.getEnabled()) {
                continue;
            }
            try {
                int count = pullSource(source);
                total += count;
                log.info("来源拉取完成 [{}]，新增 {} 篇", source.getUrl(), count);
            } catch (Exception e) {
                log.error("来源拉取失败 [{}]: {}", source.getUrl(), e.getMessage());
            }
        }
        return total;
    }

    private int pullSource(CrawlSource source) {
        List<RawPost> posts = fetchByType(source);
        int count = 0;
        for (int i = 0; i < posts.size(); i++) {
            if (saveQuietly(posts.get(i), source.getCategoryName())) {
                count++;
            }
            if (i < posts.size() - 1) {
                //同一来源逐篇限速，对目标站保持礼貌
                ThreadUtil.sleep(2000);
            }
        }
        return count;
    }

    /**
     * 手动单篇拉取（后台"爬取文章"弹窗）：
     * csdn=CSDN 文章页；html=通用网页；rss=订阅源（导入最新文章）；discourse=论坛站点（导入最新主题）
     * @return 新增文章数
     */
    public int pullManual(String url, String type) {
        if (StringUtils.isBlank(url) || !StringUtils.startsWithAny(url, "http://", "https://")) {
            throw new ServiceException("仅支持 http/https 链接");
        }
        CrawlSource source = new CrawlSource();
        source.setUrl(url);
        source.setType(StringUtils.defaultIfBlank(type, "csdn"));
        //手动拉取一律取全文，入库仍走私密待审
        source.setMode("full");
        if ("csdn".equalsIgnoreCase(source.getType())) {
            CrawlSource.Selectors selectors = new CrawlSource.Selectors();
            selectors.setTitle(".title-article");
            selectors.setContent(".article_content");
            selectors.setTags(".tag-link");
            source.setSelectors(selectors);
        }

        List<RawPost> posts = fetchByType(source);
        int count = 0;
        for (RawPost post : posts) {
            try {
                sysArticleService.saveImportedArticle(post.url, post.title, post.html, post.tags, source.getCategoryName());
                count++;
            } catch (ServiceException e) {
                if (posts.size() == 1) {
                    //单篇时把原因（如已导入过）直接抛给前端提示
                    throw e;
                }
                log.info("跳过文章 [{}]: {}", post.title, e.getMessage());
            }
        }
        return count;
    }

    private List<RawPost> fetchByType(CrawlSource source) {
        String type = StringUtils.lowerCase(source.getType());
        switch (StringUtils.defaultString(type)) {
            case "rss":
                return fetchRss(source);
            case "discourse":
                return fetchDiscourse(source);
            case "html":
            case "csdn":
                return fetchHtml(source);
            default:
                throw new ServiceException("不支持的来源类型: " + source.getType());
        }
    }

    private boolean saveQuietly(RawPost post, String categoryName) {
        try {
            sysArticleService.saveImportedArticle(post.url, post.title, post.html, post.tags, categoryName);
            return true;
        } catch (ServiceException e) {
            log.info("跳过文章 [{}]: {}", post.title, e.getMessage());
        } catch (Exception e) {
            log.error("保存文章失败 [{}]: {}", post.title, e.getMessage());
        }
        return false;
    }

    /**
     * RSS/Atom feed：优先用 feed 自带的正文（content:encoded 等），excerpt 模式不请求文章页
     */
    private List<RawPost> fetchRss(CrawlSource source) {
        List<RawPost> posts = new ArrayList<>();
        Document xml;
        try {
            xml = Jsoup.connect(source.getUrl()).userAgent(USER_AGENT).timeout(10000)
                    .ignoreContentType(true).get();
        } catch (IOException e) {
            throw new ServiceException("RSS 读取失败: " + source.getUrl());
        }
        Elements items = xml.select("item");
        if (items.isEmpty()) {
            items = xml.select("entry");
        }
        boolean full = "full".equalsIgnoreCase(source.getMode());
        for (Element item : items) {
            if (posts.size() >= limitOf(source)) {
                break;
            }
            String link = childLink(item);
            String title = childText(item, "title");
            if (StringUtils.isBlank(link) || StringUtils.isBlank(title)) {
                continue;
            }
            //feed 里可能已带正文：RSS 的 content:encoded/description，Atom 的 content
            String feedHtml = childText(item, "content:encoded", "content", "description");
            if (full) {
                String html;
                if (StringUtils.isNotBlank(feedHtml)) {
                    html = feedHtml;
                } else {
                    html = fetchArticleHtml(source, link);
                    //逐篇补抓正文时限速
                    ThreadUtil.sleep(1000);
                }
                if (StringUtils.isBlank(html)) {
                    log.info("feed 无正文且页面提取失败，跳过: {}", link);
                    continue;
                }
                posts.add(new RawPost(link, title, html, Collections.emptyList()));
            } else {
                String summary = StringUtils.abbreviate(Jsoup.parse(StringUtils.defaultString(feedHtml)).text(), 200);
                posts.add(new RawPost(link, title, excerptHtml(summary, link), Collections.emptyList()));
            }
        }
        return posts;
    }

    /**
     * Discourse 论坛（linux.do 等）：latest.json 拿主题列表，full 模式再逐帖取 cooked HTML。
     * 注意：论坛帖子是多作者对话，建议使用 excerpt 模式做摘要聚合
     */
    private List<RawPost> fetchDiscourse(CrawlSource source) {
        String base = StringUtils.removeEnd(source.getUrl(), "/");
        JSONObject json = JSONObject.parseObject(httpGet(base + "/latest.json"));
        JSONArray topics = json.getJSONArray("topics");
        if (topics == null || topics.isEmpty()) {
            throw new ServiceException("接口未返回主题列表（站点可能需要登录或开启了访问防护）");
        }
        boolean full = "full".equalsIgnoreCase(source.getMode());
        List<RawPost> posts = new ArrayList<>();
        for (int i = 0; i < topics.size() && posts.size() < limitOf(source); i++) {
            JSONObject topic = topics.getJSONObject(i);
            Integer id = topic.getInteger("id");
            String title = topic.getString("title");
            if (id == null || StringUtils.isBlank(title)) {
                continue;
            }
            String slug = topic.getString("slug");
            String link = base + "/t/" + (StringUtils.isNotBlank(slug) ? slug : "topic") + "/" + id;
            String html;
            if (full) {
                ThreadUtil.sleep(2000);
                String detail = httpGet(base + "/t/" + id + ".json");
                JSONObject postStream = JSONObject.parseObject(detail).getJSONObject("post_stream");
                html = postStream.getJSONArray("posts").getJSONObject(0).getString("cooked");
            } else {
                String excerpt = topic.getString("excerpt");
                //latest.json 未带摘要时不逐帖请求，避免对论坛产生 N 次访问
                if (StringUtils.isBlank(excerpt)) {
                    continue;
                }
                html = excerptHtml(StringUtils.abbreviate(Jsoup.parse(excerpt).text(), 200), link);
            }
            posts.add(new RawPost(link, title, html, Collections.emptyList()));
        }
        if (!full && posts.isEmpty()) {
            log.warn("来源 [{}] 为 excerpt 模式，但站点未返回任何摘要（Discourse 的 latest.json 默认不含 excerpt），导入 0 篇；可将该来源 mode 改为 full 或改配 RSS 源", source.getUrl());
        }
        return posts;
    }

    /**
     * 单页 HTML 配方：按 selectors 提取，缺省为 CSDN 文章页配方
     */
    private List<RawPost> fetchHtml(CrawlSource source) {
        Document doc;
        try {
            doc = Jsoup.connect(source.getUrl()).userAgent(USER_AGENT).timeout(10000).followRedirects(true).get();
        } catch (IOException e) {
            throw new ServiceException("页面读取失败: " + source.getUrl());
        }
        CrawlSource.Selectors selectors = source.getSelectors();
        String title = extractTitle(doc, selectors);
        String content = extractContent(doc, selectors);
        if (StringUtils.isBlank(content)) {
            throw new ServiceException("未识别到正文内容，请确认链接是否为文章详情页");
        }
        List<String> tags = new ArrayList<>();
        if (selectors != null && StringUtils.isNotBlank(selectors.getTags())) {
            doc.select(selectors.getTags()).forEach(el -> tags.add(el.text()));
        }
        return Collections.singletonList(new RawPost(source.getUrl(), title, content, tags));
    }

    /**
     * feed 不带全文时，读取文章页并按选择器/通用规则提取正文
     */
    private String fetchArticleHtml(CrawlSource source, String link) {
        try {
            Document doc = Jsoup.connect(link).userAgent(USER_AGENT).timeout(10000).followRedirects(true).get();
            return StringUtils.defaultString(extractContent(doc, source.getSelectors()));
        } catch (IOException e) {
            log.warn("正文页面读取失败: {}", link);
            return "";
        }
    }

    private String extractContent(Document doc, CrawlSource.Selectors selectors) {
        if (selectors != null && StringUtils.isNotBlank(selectors.getContent())) {
            Element el = doc.selectFirst(selectors.getContent());
            return el != null ? el.html() : "";
        }
        //通用兜底：article 标签及常见博客平台的正文容器
        for (String css : Arrays.asList(
                "article",
                "#cnblogs_post_body", ".blogpost-body",   // 博客园
                ".article_content",                       // CSDN
                ".article-content", ".markdown-body",     // 掘金等
                ".entry-content", ".post-content",        // WordPress 等
                "#js_content", ".rich_media_content"      // 微信公众号
        )) {
            Element el = doc.selectFirst(css);
            if (el != null) {
                return el.html();
            }
        }
        return "";
    }

    /**
     * 标题提取：配置选择器优先，其次正文 h1（多数博客的文章标题），最后页面 title
     */
    private String extractTitle(Document doc, CrawlSource.Selectors selectors) {
        if (selectors != null && StringUtils.isNotBlank(selectors.getTitle())) {
            Element el = doc.selectFirst(selectors.getTitle());
            if (el != null) {
                return el.text();
            }
        }
        Element h1 = doc.selectFirst("h1");
        if (h1 != null && StringUtils.isNotBlank(h1.text())) {
            return h1.text();
        }
        return doc.title();
    }

    private String excerptHtml(String summary, String link) {
        return "<p>" + Jsoup.parse(StringUtils.defaultString(summary)).text() + "</p>"
                + "<p>阅读原文：<a href=\"" + link + "\" rel=\"noopener nofollow\" target=\"_blank\">" + link + "</a></p>";
    }

    private int limitOf(CrawlSource source) {
        int limit = source.getMaxItems() == null ? 10 : source.getMaxItems();
        return Math.min(Math.max(limit, 1), MAX_ITEMS_CAP);
    }

    private String httpGet(String url) {
        String body = HttpRequest.get(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .timeout(10000)
                .execute().body();
        if (StringUtils.isBlank(body)) {
            throw new ServiceException("接口无响应: " + url);
        }
        return body;
    }

    /**
     * 取直接子元素中首个匹配标签的文本（兼容 content:encoded 这类带命名空间前缀的标签）。
     * 按 names 顺序优先匹配（而非文档顺序），保证 content:encoded 优先于 description
     */
    private String childText(Element parent, String... names) {
        for (String name : names) {
            for (Element child : parent.children()) {
                String tag = child.tagName().toLowerCase();
                if (tag.equals(name) || tag.endsWith(":" + name)) {
                    return child.text();
                }
            }
        }
        return "";
    }

    /**
     * 取条目链接：Atom 条目可能有 alternate/self/replies 多个 link，
     * 优先取 rel="alternate" 或未标注 rel 的，避免取到 self 等辅助链接
     */
    private String childLink(Element parent) {
        String fallback = "";
        for (Element child : parent.children()) {
            if (!child.tagName().toLowerCase().endsWith("link")) {
                continue;
            }
            String href = child.attr("href");
            if (StringUtils.isBlank(href)) {
                href = child.text();
            }
            if (StringUtils.isBlank(href)) {
                continue;
            }
            String rel = child.attr("rel");
            if (StringUtils.isBlank(rel) || "alternate".equalsIgnoreCase(rel)) {
                return href;
            }
            if (StringUtils.isBlank(fallback)) {
                fallback = href;
            }
        }
        return fallback;
    }

    /**
     * 单篇抓取结果
     */
    static class RawPost {

        final String url;
        final String title;
        final String html;
        final List<String> tags;

        RawPost(String url, String title, String html, List<String> tags) {
            this.url = url;
            this.title = title;
            this.html = html;
            this.tags = tags;
        }
    }
}
