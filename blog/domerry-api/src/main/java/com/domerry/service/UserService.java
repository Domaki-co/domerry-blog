package com.domerry.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.domerry.entity.SysArticle;
import com.domerry.entity.SysFeedback;
import com.domerry.entity.SysUser;
import com.domerry.vo.article.ArticleListVo;
import com.domerry.vo.comment.CommentListVo;

import java.util.List;

/**
 * @author: quequnlong
 * @date: 2025/1/11
 * @description:
 */
public interface UserService {

    /**
     * 查询我的评论
     * @return
     */
    IPage<CommentListVo> selectMyComment();

    /**
     * 删除我的评论
     * @param ids
     * @return
     */
    Void delMyComment(List<Long> ids);

    /**
     * 查询我的点赞
     * @return
     */
    IPage<ArticleListVo> selectMyLike();

    /**
     * 查询我的回复
     * @return
     */
    IPage<CommentListVo> getMyReply();

    /**
     * 修改我的资料
     * @param user
     */
    void updateProfile(SysUser user);

    /**
     * 查询我的文章
     * @return
     */
    IPage<ArticleListVo> selectMyArticle(SysArticle article);

    /**
     * 更新当前登录用户的偏好设置
     * @param settings 新的个性化偏好键值对
     * @return 最新的完整个性化偏好
     */
    java.util.Map<String, Object> updateSettings(java.util.Map<String, Object> settings);

}
