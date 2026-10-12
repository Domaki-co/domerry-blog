package com.domerry.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domerry.vo.dashboard.ContributionData;
import com.domerry.dto.article.ArticleQueryDto;
import com.domerry.vo.article.ArticleDetailVo;
import com.domerry.vo.article.ArticleListVo;
import com.domerry.entity.SysArticle;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 文章 Mapper接口
 */
@Mapper
public interface SysArticleMapper extends BaseMapper<SysArticle> {

    /**
     * 门户-获取文章列表
     * @param page
     * @param tagId
     * @param categoryId
     * @param viewUserId 当前登录用户id（未登录为 null），用于作者可见自己的私密文章
     * @return
     */
    IPage<ArticleListVo> getArticleListApi(@Param("page") Page<Object> page,@Param("tagId") Integer tagId,
                                           @Param("categoryId")Integer categoryId, @Param("keyword")String keyword,
                                           @Param("viewUserId") Long viewUserId);

    /**
     * 获取文章详情
     * @param id
     * @return
     */
    ArticleDetailVo getArticleDetail(Long id);

    /**
     * 获取文章归档
     * @param viewUserId 当前登录用户id（未登录为 null），用于作者可见自己的私密文章
     * @return
     */
    List<Integer> getArticleArchive(@Param("viewUserId") Long viewUserId);

    /**
     * 获取文章列表按年分组
     * @param year
     * @param viewUserId 当前登录用户id（未登录为 null），用于作者可见自己的私密文章
     * @return
     */
    List<ArticleListVo> getArticleByYear(@Param("year") Integer year, @Param("viewUserId") Long viewUserId);

    /**
     * 自定义分页查询
     * @param page
     * @param articleQueryDto
     * @return
     */
    IPage<ArticleListVo> selectPageList(@Param("page") Page<Object> page, @Param("query") ArticleQueryDto articleQueryDto);

    /**
     * 获取今年贡献度
     * @return
     */
    List<ContributionData> getThisYearContributionData();

    /**
     * 获取用户是否文章点赞
     * @param articleId
     * @param userId
     * @return
     */
    Boolean getUserIsLike(@Param("articleId") Long articleId, @Param("userId") int userId);

    /**
     * 文章取消点赞
     * @param articleId
     * @param userId
     */
    void unLike(@Param("articleId")Long articleId, @Param("userId")int userId);

    /**
     * 文章点赞
     * @param articleId
     * @param userId
     */
    void like(@Param("articleId")Long articleId,@Param("userId")int userId);

    @MapKey("name")
    List<Map<String, Integer>> selectCountByCategory();

    IPage<ArticleListVo> selectMyLike(@Param("page")Page<Object> page, @Param("userId")long userId);

    IPage<ArticleListVo> selectMyArticle(@Param("page")Page<Object> page,@Param("article") SysArticle article);

    void updateBatchQuantity(@Param("articles") List<SysArticle> articles);
}
