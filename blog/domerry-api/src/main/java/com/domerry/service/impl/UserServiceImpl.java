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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public java.util.Map<String, Object> updateSettings(java.util.Map<String, Object> newSettings) {
        Integer userId = StpUtil.getLoginIdAsInt();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new com.domerry.exception.ServiceException("用户不存在");
        }

        java.util.Map<String, Object> currentMap = new java.util.HashMap<>();
        if (user.getUserSettings() != null && !user.getUserSettings().trim().isEmpty()) {
            try {
                java.util.Map<?, ?> parsed = com.alibaba.fastjson.JSON.parseObject(user.getUserSettings(), java.util.Map.class);
                if (parsed != null) {
                    for (java.util.Map.Entry<?, ?> entry : parsed.entrySet()) {
                        currentMap.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (newSettings != null) {
            currentMap.putAll(newSettings);
        }

        String jsonStr = com.alibaba.fastjson.JSON.toJSONString(currentMap);
        user.setUserSettings(jsonStr);
        sysUserMapper.updateById(user);

        // 同步当前 Session 缓存
        try {
            cn.dev33.satoken.session.SaSession session = StpUtil.getSession(false);
            if (session != null) {
                com.domerry.dto.user.LoginUserInfo currentUser = (com.domerry.dto.user.LoginUserInfo) session.get(com.domerry.common.Constants.CURRENT_USER);
                if (currentUser != null) {
                    currentUser.setUserSettings(currentMap);
                    session.set(com.domerry.common.Constants.CURRENT_USER, currentUser);
                }
            }
        } catch (Exception ignored) {
        }

        return currentMap;
    }

}
