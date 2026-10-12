package com.domerry.controller.seo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.domerry.entity.SysArticle;
import com.domerry.mapper.SysArticleMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * sitemap.xml 动态生成：静态页 + 已发布文章，供搜索引擎抓取
 * 站点域名通过 seo.site-url 配置
 */
@RestController
@RequiredArgsConstructor
@Api(tags = "SEO")
public class SitemapController {

    private final SysArticleMapper sysArticleMapper;

    private static final DateTimeFormatter LASTMOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Value("${seo.site-url:https://your-domain.example}")
    private String siteUrl;

    @GetMapping(value = "/api/sitemap.xml", produces = "application/xml;charset=UTF-8")
    @ApiOperation(value = "站点地图", produces = "application/xml")
    public String sitemap() {
        String base = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;

        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 静态页（登录、个人中心、聊天等用户态页面不收录）
        String[] staticPaths = {"/", "/archive", "/categories", "/tags", "/moments", "/photos",
                "/resources", "/messages", "/friends", "/about"};
        for (String path : staticPaths) {
            addUrl(xml, base + path, null);
        }

        // 已发布文章（status：0 下架 / 1 发布 / 3 审核）
        List<SysArticle> articles = sysArticleMapper.selectList(new LambdaQueryWrapper<SysArticle>()
                .select(SysArticle::getId, SysArticle::getCreateTime, SysArticle::getUpdateTime)
                .eq(SysArticle::getStatus, 1)
                .orderByDesc(SysArticle::getId));
        for (SysArticle article : articles) {
            addUrl(xml, base + "/post/" + article.getId(),
                    article.getUpdateTime() != null ? article.getUpdateTime() : article.getCreateTime());
        }

        xml.append("</urlset>");
        return xml.toString();
    }

    private void addUrl(StringBuilder xml, String loc, LocalDateTime lastmod) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        if (lastmod != null) {
            xml.append("    <lastmod>").append(LASTMOD_FORMAT.format(lastmod)).append("</lastmod>\n");
        }
        xml.append("  </url>\n");
    }

    private String escapeXml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
