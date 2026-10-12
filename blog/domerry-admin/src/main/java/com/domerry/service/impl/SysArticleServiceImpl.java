package com.domerry.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domerry.common.Constants;
import com.domerry.dto.article.ArticleQueryDto;
import com.domerry.entity.SysArticle;
import com.domerry.entity.SysCategory;
import com.domerry.entity.SysConfig;
import com.domerry.entity.SysTag;
import com.domerry.exception.ServiceException;
import com.domerry.mapper.SysArticleMapper;
import com.domerry.mapper.SysCategoryMapper;
import com.domerry.mapper.SysTagMapper;
import com.domerry.service.ArticleAiSummaryService;
import com.domerry.service.SysArticleService;
import com.domerry.service.SysConfigService;
import com.domerry.utils.PageUtil;
import com.domerry.vo.article.ArticleListVo;
import com.domerry.vo.article.SysArticleDetailVo;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysArticleServiceImpl extends ServiceImpl<SysArticleMapper, SysArticle> implements SysArticleService {

    private final SysTagMapper sysTagMapper;

    private final ArticleAiSummaryService articleAiSummaryService;
    private final SysCategoryMapper sysCategoryMapper;
    private final SysConfigService sysConfigService;

    @Override
    public IPage<ArticleListVo> selectPage(ArticleQueryDto articleQueryDto) {
        return baseMapper.selectPageList(PageUtil.getPage(), articleQueryDto);
    }

    @Override
    public SysArticleDetailVo detail(Integer id) {
        SysArticle sysArticle = baseMapper.selectById(id);

        SysArticleDetailVo sysArticleDetailVo = new SysArticleDetailVo();
        BeanUtils.copyProperties(sysArticle, sysArticleDetailVo);

        SysCategory sysCategory = sysCategoryMapper.selectById(sysArticle.getCategoryId());
        sysArticleDetailVo.setCategoryName(ObjectUtils.isEmpty(sysCategory) ? "" : sysCategory.getName());

        //获取标签
        List<String> tags = sysTagMapper.getTagNameByArticleId(id);
        sysArticleDetailVo.setTags(tags);
        return sysArticleDetailVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean add(SysArticleDetailVo sysArticle) {

        SysArticle obj = new SysArticle();
        BeanUtils.copyProperties(sysArticle, obj);
        obj.setUserId(StpUtil.getLoginIdAsLong());

        //非管理员不允许直接发布，只能保存草稿或提交审核
        if (!StpUtil.hasRole(Constants.ADMIN) && !Integer.valueOf(Constants.ARTICLE_DRAFT).equals(obj.getStatus())) {
            obj.setStatus(Constants.ARTICLE_REVIEW);
        }

        //非管理员不允许置顶（防止越权：首页置顶栏是人工维护的常用入口）
        if (!StpUtil.hasRole(Constants.ADMIN)) {
            obj.setIsStick(0);
        }

        //添加分类
        addCategory(sysArticle, obj);
        baseMapper.insert(obj);

        addTags(sysArticle, obj);

        if (Boolean.TRUE.equals(sysArticle.getRegenerateAiDescribe())) {
            articleAiSummaryService.generate(obj.getId());
        }
        return true;
    }




    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(SysArticleDetailVo sysArticle) {

        SysArticle obj = new SysArticle();
        BeanUtils.copyProperties(sysArticle, obj);

        //没有管理员权限就只能修改自己的文章
        if (!StpUtil.hasRole(Constants.ADMIN)) {
            SysArticle article = baseMapper.selectById(sysArticle.getId());
            if (article == null) {
                throw new ServiceException("文章不存在");
            }
            if (!Objects.equals(article.getUserId(), StpUtil.getLoginIdAsLong())) {
                throw new ServiceException("只能修改自己的文章");
            }
            //非管理员不能通过内容编辑接口变更状态（防止越权发布）：草稿保持草稿，其余一律回审核
            obj.setStatus(Integer.valueOf(Constants.ARTICLE_DRAFT).equals(sysArticle.getStatus())
                    ? Constants.ARTICLE_DRAFT : Constants.ARTICLE_REVIEW);
            //非管理员不能通过内容编辑接口变更置顶（防止越权置顶）：保持文章原有的置顶状态
            obj.setIsStick(article.getIsStick());
        }

        addCategory(sysArticle, obj);
        baseMapper.updateById(obj);

        //先删除标签在新增标签
        sysTagMapper.deleteArticleTagsByArticleIds(Collections.singletonList(obj.getId()));
        addTags(sysArticle, obj);

        if (Boolean.TRUE.equals(sysArticle.getRegenerateAiDescribe())) {
            articleAiSummaryService.generate(obj.getId());
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(List<Long> ids) {

        //没有管理员权限就只能删除自己的文章
        if (!StpUtil.hasRole(Constants.ADMIN)) {
            List<SysArticle> sysArticles = baseMapper.selectBatchIds(ids);
            for (SysArticle sysArticle : sysArticles) {
                if (sysArticle.getUserId() != StpUtil.getLoginIdAsLong()) {
                    throw new RuntimeException("只能删除自己的文章");
                }
            }
        }

        baseMapper.deleteBatchIds(ids);
        sysTagMapper.deleteArticleTagsByArticleIds(ids);
        return true;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveImportedArticle(String url, String title, String contentHtml, List<String> tags, String categoryName) {
        if (StringUtils.isBlank(url) || !StringUtils.startsWithAny(url, "http://", "https://")) {
            throw new ServiceException("仅支持 http/https 链接");
        }
        //同一原文只允许导入一次，避免重复抓取产生重复文章
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysArticle>().eq(SysArticle::getOriginalUrl, url));
        if (count != null && count > 0) {
            throw new ServiceException("该文章已导入过，请勿重复抓取");
        }

        //白名单消毒：只保留正文标签，去除第三方页面里不可信的内容
        String cleanHtml = Jsoup.clean(contentHtml, Safelist.relaxed()
                .addAttributes(":all", "class", "style"));
        String markdown = FlexmarkHtmlConverter.builder(new MutableDataSet()).build().convert(cleanHtml);

        SysArticle entity = SysArticle.builder()
                .contentMd(markdown)
                .isOriginal(Constants.NO).originalUrl(url)
                .title(StringUtils.defaultIfBlank(title, "未命名文章")).content(cleanHtml)
                //默认落私密，人工确认后再公开
                .status(Constants.ARTICLE_PRIVATE)
                .build();
        entity.setUserId(resolveImportUserId());
        //归入指定分类，缺省"转载"
        String catName = StringUtils.defaultIfBlank(categoryName, "转载");
        SysCategory category = sysCategoryMapper.selectOne(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getName, catName));
        if (category == null) {
            category = SysCategory.builder().name(catName).build();
            sysCategoryMapper.insert(category);
        }
        entity.setCategoryId(category.getId());
        try {
            baseMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            //并发窗口下由唯一索引（uk_original_url）兜底，转为友好提示
            throw new ServiceException("该文章已导入过，请勿重复抓取");
        }

        //为该文章添加标签（源站没有标签时不强制填充）
        List<Integer> tagIds = new ArrayList<>();
        if (tags != null) {
            for (String tag : tags) {
                if (StringUtils.isBlank(tag)) continue;
                SysTag result = sysTagMapper.selectOne(new LambdaQueryWrapper<SysTag>().eq(SysTag::getName, tag));
                if (result == null) {
                    result = SysTag.builder().name(tag).build();
                    sysTagMapper.insert(result);
                }
                tagIds.add(result.getId());
            }
        }
        if (!tagIds.isEmpty()) {
            sysTagMapper.addArticleTagRelations(entity.getId(), tagIds);
        }
        log.info("导入文章成功: [{}] <- {}", entity.getTitle(), url);
        return entity.getId();
    }

    /**
     * 导入文章的归属用户：手动爬取取当前登录人，定时任务无登录态时取 sys_config 的 crawl.userId（缺省 1，站长）
     */
    private Long resolveImportUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            SysConfig config = sysConfigService.selectConfigByKey("crawl.userId");
            try {
                return config != null && StringUtils.isNotBlank(config.getConfigValue())
                        ? Long.valueOf(config.getConfigValue().trim()) : 1L;
            } catch (NumberFormatException nfe) {
                log.warn("sys_config crawl.userId 配置非法，使用默认 1: {}", config.getConfigValue());
                return 1L;
            }
        }
    }

    private void addCategory(SysArticleDetailVo sysArticle, SysArticle obj) {
        SysCategory sysCategory = sysCategoryMapper.selectOne(new LambdaQueryWrapper<SysCategory>()
                .eq(SysCategory::getName, sysArticle.getCategoryName()));
        if (sysCategory == null) {
            sysCategory = SysCategory.builder().name(sysArticle.getCategoryName()).build();
            sysCategoryMapper.insert(sysCategory);
        }
        obj.setCategoryId(sysCategory.getId());
    }

    private void addTags(SysArticleDetailVo sysArticle, SysArticle obj) {
        //添加标签
        List<Integer> tagIds = new ArrayList<>();
        for (String tag : sysArticle.getTags()) {
            SysTag sysTag = sysTagMapper.selectOne(new LambdaQueryWrapper<SysTag>().eq(SysTag::getName, tag));
            if (sysTag == null) {
                sysTag = SysTag.builder().name(tag).build();
                sysTagMapper.insert(sysTag);
            }
            tagIds.add(sysTag.getId());
        }
        sysTagMapper.addArticleTagRelations(obj.getId(), tagIds);
    }
}
