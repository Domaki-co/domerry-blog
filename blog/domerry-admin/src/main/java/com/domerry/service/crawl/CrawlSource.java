package com.domerry.service.crawl;

import lombok.Data;

import java.util.List;

/**
 * 抓取来源配置，来源于 sys_config 的 crawl.sources（JSON 数组），示例：
 * <pre>
 * [
 *   {"type":"rss","url":"https://someblog.com/feed","mode":"full","categoryName":"技术分享"},
 *   {"type":"html","url":"https://blog.csdn.net/xxx/article/details/1",
 *      "selectors":{"title":".title-article","content":".article_content","tags":".tag-link"}},
 *   {"type":"discourse","url":"https://linux.do","mode":"excerpt","maxItems":10,"enabled":false}
 * ]
 * </pre>
 */
@Data
public class CrawlSource {

    /** 来源类型：rss / html / discourse */
    private String type;

    /** rss 为 feed 地址；html 为文章页地址；discourse 为站点根地址 */
    private String url;

    /** 入库模式：full 抓全文（仍落私密待人工确认）；excerpt 只存摘要+原文链接。缺省 excerpt */
    private String mode;

    /** 归入的分类名，缺省"转载" */
    private String categoryName;

    /** 单次拉取条数上限（1~20），缺省 10 */
    private Integer maxItems;

    /** 是否启用，缺省 true */
    private Boolean enabled;

    /** type=html 时的页面选择器，缺省为 CSDN 文章页配方 */
    private Selectors selectors;

    @Data
    public static class Selectors {

        /** 标题选择器 */
        private String title;

        /** 正文选择器 */
        private String content;

        /** 标签选择器 */
        private String tags;
    }
}
