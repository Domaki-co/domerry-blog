package com.domerry.service;

/**
 * 文章 AI 摘要生成服务。
 */
public interface ArticleAiSummaryService {

    /**
     * 用户主动选择生成/重新生成摘要，在事务提交后异步执行。
     *
     * @param articleId 文章 id
     */
    void generate(Long articleId);

    /**
     * 测试当前数据库中的 AI 接口配置。
     *
     * @return AI 返回内容
     */
    String testConnection();
}
