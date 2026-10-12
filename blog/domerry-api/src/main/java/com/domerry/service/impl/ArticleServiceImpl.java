package com.domerry.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.domerry.common.Constants;
import com.domerry.common.RedisConstants;
import com.domerry.entity.SysArticle;
import com.domerry.entity.SysCategory;
import com.domerry.entity.SysNotifications;
import com.domerry.exception.ServiceException;
import com.domerry.service.ArticleService;
import com.domerry.utils.IpUtil;
import com.domerry.utils.NotificationsUtil;
import com.domerry.utils.RedisUtil;
import com.domerry.vo.article.ArchiveListVo;
import com.domerry.vo.article.ArticleDetailVo;
import com.domerry.vo.article.ArticleListVo;
import com.domerry.vo.article.CategoryListVo;
import com.domerry.mapper.SysArticleMapper;
import com.domerry.mapper.SysCategoryMapper;
import com.domerry.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    /**
     * 首页置顶快捷栏最多展示条数，置顶是"常用入口"而非内容列表，过多会挤占正文
     */
    private static final int STICK_LIMIT = 5;

    private final SysArticleMapper sysArticleMapper;

    private final SysCategoryMapper sysCategoryMapper;

    private final RedisUtil redisUtil;

    private final NotificationsUtil notificationsUtil;

    @Override
    public IPage<ArticleListVo> getArticleList(Integer tagId, Integer categoryId, String keyword) {
        return sysArticleMapper.getArticleListApi(PageUtil.getPage(), tagId, categoryId, keyword, getViewUserId());
    }

    @Override
    public ArticleDetailVo getArticleDetail(Long id) {
        SysArticle article = sysArticleMapper.selectById(id);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        //非发布状态的文章（草稿/审核/下架/私密）仅作者本人和管理员可见
        if (!Integer.valueOf(Constants.ARTICLE_PUBLISHED).equals(article.getStatus())) {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            boolean isAuthor = loginId != null && article.getUserId() != null
                    && article.getUserId().equals(Long.valueOf(loginId.toString()));
            //先确认已登录再查角色，未登录时 hasRole 会抛 NotLoginException
            boolean isAdmin = loginId != null && StpUtil.hasRole(Constants.ADMIN);
            if (!isAuthor && !isAdmin) {
                throw new ServiceException("该内容仅作者可见");
            }
        }

        ArticleDetailVo detailVo = sysArticleMapper.getArticleDetail(id);
        // 判断是否点赞
        Object userId = StpUtil.getLoginIdDefaultNull();
        if (userId != null) {
            detailVo.setIsLike(sysArticleMapper.getUserIsLike(id, Integer.parseInt(userId.toString())));
        }

        //添加阅读量
        String ip = IpUtil.getIp();
        ThreadUtil.execAsync(() -> {
            Map<Object, Object> map = redisUtil.hGetAll(RedisConstants.ARTICLE_QUANTITY);
            List<String> ipList = (List<String>) map.get(id.toString());
            if (ipList != null) {
                if (!ipList.contains(ip)) {
                    ipList.add(ip);
                }
            } else {
                ipList = new ArrayList<>();
                ipList.add(ip);
            }
            map.put(id.toString(), ipList);
            redisUtil.hSetAll(RedisConstants.ARTICLE_QUANTITY, map);
        });
        return detailVo;
    }

    @Override
    public List<ArchiveListVo> getArticleArchive() {
        Long viewUserId = getViewUserId();
        List<ArchiveListVo> list = new ArrayList<>();

        List<Integer> years = sysArticleMapper.getArticleArchive(viewUserId);
        for (Integer year : years) {
            List<ArticleListVo> articleListVos = sysArticleMapper.getArticleByYear(year, viewUserId);
            list.add(new ArchiveListVo(year, articleListVos));
        }
        return list;
    }

    @Override
    public List<CategoryListVo> getArticleCategories() {
        return sysCategoryMapper.getArticleCategories();
    }

    @Override
    public List<ArticleListVo> getCarouselArticle() {
        return getArticlesByCondition(SysArticle::getIsCarousel);
    }

    @Override
    public List<ArticleListVo> getRecommendArticle() {
        return getArticlesByCondition(SysArticle::getIsRecommend);
    }

    @Override
    public List<ArticleListVo> getStickArticle() {
        //置顶允许包含作者本人的私密文章，门户置顶栏里这些条目只有作者本人能看到
        return getArticlesByCondition(SysArticle::getIsStick, STICK_LIMIT, true);
    }

    @Override
    public Boolean like(Long articleId) {
        // 判断是否点赞
        int userId = StpUtil.getLoginIdAsInt();
        Boolean isLike = sysArticleMapper.getUserIsLike(articleId, userId);
        if (isLike) {
            // 点过则取消点赞
            sysArticleMapper.unLike(articleId, userId);
        } else {
            sysArticleMapper.like(articleId, userId);
            ThreadUtil.execAsync(() -> {
                //发送通知事件
                SysNotifications notifications = SysNotifications.builder()
                        .title("文章点赞通知")
                        .articleId(articleId)
                        .isRead(0)
                        .type("like")
                        .fromUserId(StpUtil.getLoginIdAsLong())
                        .build();
                notificationsUtil.publish(notifications);
            });
        }
        return true;
    }

    @Override
    public List<SysCategory> getCategoryAll() {
        return sysCategoryMapper.selectList(new LambdaQueryWrapper<SysCategory>()
                .orderByAsc(SysCategory::getSort));
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        SysArticle article = sysArticleMapper.selectById(id);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }

        boolean isAdmin = StpUtil.hasRole(Constants.ADMIN);
        if (!isAdmin && !article.getUserId().equals(StpUtil.getLoginIdAsLong())) {
            throw new ServiceException("只能操作自己的文章");
        }

        //status 库里可空，默认按 0（下架）处理
        int current = article.getStatus() == null ? Constants.ARTICLE_OFFLINE : article.getStatus();
        if (isAdmin) {
            if (status != Constants.ARTICLE_PUBLISHED && status != Constants.ARTICLE_OFFLINE) {
                throw new ServiceException("不支持的状态变更");
            }
        } else {
            //非管理员：草稿/下架可提交审核，已发布/审核中可下架，发布必须经管理员审核
            boolean toReview = (current == Constants.ARTICLE_DRAFT || current == Constants.ARTICLE_OFFLINE)
                    && status == Constants.ARTICLE_REVIEW;
            boolean toOffline = (current == Constants.ARTICLE_PUBLISHED || current == Constants.ARTICLE_REVIEW)
                    && status == Constants.ARTICLE_OFFLINE;
            if (!toReview && !toOffline) {
                throw new ServiceException("当前状态不支持该操作");
            }
        }

        SysArticle update = new SysArticle();
        update.setId(id);
        update.setStatus(status);
        sysArticleMapper.updateById(update);
    }

    /**
     * 当前登录用户id，未登录返回 null；用于让作者在列表/归档中可见自己的私密文章
     */
    private Long getViewUserId() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        return loginId == null ? null : Long.valueOf(loginId.toString());
    }

    private List<ArticleListVo> getArticlesByCondition(SFunction<SysArticle, Object> conditionField) {
        return getArticlesByCondition(conditionField, null, false);
    }

    private List<ArticleListVo> getArticlesByCondition(SFunction<SysArticle, Object> conditionField, Integer limit) {
        return getArticlesByCondition(conditionField, limit, false);
    }

    /**
     * 轮播/推荐/置顶的通用查询
     * @param conditionField 标记字段（is_carousel / is_recommend / is_stick）
     * @param limit 可选条数上限，null 表示不限
     * @param allowOwnPrivate 是否允许出现「当前登录用户自己的私密文章」，仅置顶开启
     */
    private List<ArticleListVo> getArticlesByCondition(SFunction<SysArticle, Object> conditionField, Integer limit,
                                                       boolean allowOwnPrivate) {
        LambdaQueryWrapper<SysArticle> wrapper = new LambdaQueryWrapper<SysArticle>()
                .select(SysArticle::getId, SysArticle::getTitle, SysArticle::getCover, SysArticle::getCreateTime)
                .orderByDesc(SysArticle::getCreateTime)
                .eq(conditionField, 1);

        if (allowOwnPrivate) {
            //置顶的状态口径与列表页保持一致：上架文章所有人可见，私密文章仅作者本人可见
            Long viewUserId = getViewUserId();
            if (viewUserId == null) {
                wrapper.eq(SysArticle::getStatus, Constants.ARTICLE_PUBLISHED);
            } else {
                wrapper.and(w -> w.eq(SysArticle::getStatus, Constants.ARTICLE_PUBLISHED)
                        .or(o -> o.eq(SysArticle::getStatus, Constants.ARTICLE_PRIVATE)
                                .eq(SysArticle::getUserId, viewUserId)));
            }
        } else {
            //轮播/推荐只允许已发布文章，避免私密文章泄露标题和封面
            wrapper.eq(SysArticle::getStatus, Constants.ARTICLE_PUBLISHED);
        }

        //limit 来自本类常量而非入参，直接拼接不存在注入风险
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }

        List<SysArticle> sysArticles = sysArticleMapper.selectList(wrapper);

        if (sysArticles == null || sysArticles.isEmpty()) {
            return Collections.emptyList();
        }

        return sysArticles.stream().map(item -> ArticleListVo.builder()
                .id(item.getId())
                .cover(item.getCover())
                .title(item.getTitle())
                .createTime(item.getCreateTime())
                .build()).collect(Collectors.toList());
    }
}
