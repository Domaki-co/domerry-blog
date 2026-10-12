package com.domerry.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.domerry.entity.SysCategory;
import com.domerry.vo.article.ArchiveListVo;
import com.domerry.vo.article.ArticleDetailVo;
import com.domerry.vo.article.ArticleListVo;
import com.domerry.vo.article.CategoryListVo;

import java.util.List;

public interface ArticleService {
    /**
     * 获取文章列表
     * @param tagId
     * @param categoryId
     * @return
     */
    IPage<ArticleListVo> getArticleList(Integer tagId, Integer categoryId,String keyword);

    /**
     * 获取文章详情
     * @param id 文章id
     * @return
     */
    ArticleDetailVo getArticleDetail(Long id);


    /**
     * 获取文章归档
     * @return
     */
    List<ArchiveListVo> getArticleArchive();

    /**
     * 获取分类
     * @return
     */
    List<CategoryListVo> getArticleCategories();

    /**
     * 获取轮播文章
     * @return
     */
    List<ArticleListVo> getCarouselArticle();

    /**
     * 获取推荐文章
     * @return
     */
    List<ArticleListVo> getRecommendArticle();

    /**
     * 获取置顶文章（首页顶部快捷入口，最多 STICK_LIMIT 条）
     * @return
     */
    List<ArticleListVo> getStickArticle();

    /**
     * 点赞文章
     * @param id
     * @return
     */
    Boolean like(Long id);

    List<SysCategory> getCategoryAll();

    /**
     * 变更我的文章状态（归属校验 + 状态机校验）
     * @param id 文章id
     * @param status 目标状态 0：下架 1：发布 3：审核
     */
    void changeStatus(Long id, Integer status);

}
