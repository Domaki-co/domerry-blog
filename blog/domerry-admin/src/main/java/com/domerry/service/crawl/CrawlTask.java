package com.domerry.service.crawl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 定时任务入口：后台"定时任务"页面新增任务，调用目标填 crawlTask.pullAll() 即可
 */
@Slf4j
@Component("crawlTask")
@RequiredArgsConstructor
public class CrawlTask {

    private final CrawlService crawlService;

    public void pullAll() {
        int count = crawlService.pullAll();
        log.info("定时拉取结束，本次新增 {} 篇", count);
    }
}
