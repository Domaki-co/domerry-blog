package com.domerry.controller.article;

import com.domerry.common.Result;
import com.domerry.service.ArticleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门户-我的文章管理（登录即可访问，归属校验在 service 中完成）
 */
@RestController
@RequestMapping("/protal/article")
@RequiredArgsConstructor
@Api(tags = "门户-我的文章")
public class PortalArticleController {

    private final ArticleService articleService;

    @PutMapping("/changeStatus/{id}/{status}")
    @ApiOperation(value = "变更我的文章状态（0：下架 1：发布 3：审核）")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        articleService.changeStatus(id, status);
        return Result.success();
    }
}
