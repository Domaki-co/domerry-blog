package com.domerry.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.domerry.entity.SysArticle;
import com.domerry.entity.SysComment;
import com.domerry.entity.SysUser;
import com.domerry.mapper.*;
import com.domerry.service.UserService;
import com.domerry.utils.PageUtil;
import com.domerry.vo.article.ArticleListVo;
import com.domerry.vo.comment.CommentListVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author: quequnlong
 * @date: 2025/1/11
 * @description:
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;

    private final SysCommentMapper commentMapper;

    private final SysArticleMapper articleMapper;

    private final SysTagMapper tagMapper;

    @Override
    public IPage<CommentListVo> selectMyComment() {
        return commentMapper.selectMyComment(PageUtil.getPage(), StpUtil.getLoginIdAsLong());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Void delMyComment(List<Long> ids) {
        commentMapper.deleteBatchIds(ids);
        commentMapper.delete(new LambdaQueryWrapper<SysComment>()
                .in(SysComment::getParentId, ids));
        return null;
    }

    @Override
    public IPage<ArticleListVo> selectMyLike() {
        return articleMapper.selectMyLike(PageUtil.getPage(),StpUtil.getLoginIdAsLong());
    }

    @Override
    public IPage<CommentListVo> getMyReply() {
        return commentMapper.getMyReply(PageUtil.getPage(),StpUtil.getLoginIdAsLong());
    }

    @Override
    public void updateProfile(SysUser user) {
        sysUserMapper.updateById(user);
    }

    @Override
    public IPage<ArticleListVo> selectMyArticle(SysArticle article) {
        article.setUserId(StpUtil.getLoginIdAsLong());
        return articleMapper.selectMyArticle(PageUtil.getPage(),article);
    }

}
