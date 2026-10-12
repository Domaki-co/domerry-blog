package com.domerry.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domerry.dto.article.ArticleQueryDto;
import com.domerry.entity.SysArticle;
import com.domerry.vo.article.ArticleListVo;
import com.domerry.vo.article.SysArticleDetailVo;

import java.util.List;

public interface SysArticleService extends IService<SysArticle> {

    /**
     * 分页查询
     * @param articleQueryDto
     * @return
     */
    IPage<ArticleListVo> selectPage(ArticleQueryDto articleQueryDto);

    /**
     * 文章详情
     * @param id
     * @return
     */
    SysArticleDetailVo detail(Integer id);

    /**
     * 新增
     * @param sysArticle
     * @return
     */
    Boolean add(SysArticleDetailVo sysArticle);

    /**
     * 修改
     * @param sysArticle
     * @return
     */
    Boolean update(SysArticleDetailVo sysArticle);


    /**
     * 删除
     * @param ids
     * @return
     */
    Boolean delete(List<Long> ids);

    /**
     * 保存抓取的外部文章：去重、消毒、默认私密，供手动爬取与定时来源拉取共用
     * @param url 原文链接（去重依据）
     * @param title 标题
     * @param contentHtml 正文 HTML（入库前会做白名单消毒）
     * @param tags 标签名列表，可为 null
     * @param categoryName 归入的分类名，可为 null（缺省"转载"）
     * @return 文章 id
     */
    Long saveImportedArticle(String url, String title, String contentHtml, List<String> tags, String categoryName);
}
