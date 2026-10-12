package com.domerry.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.domerry.common.Result;
import com.domerry.service.ArticleAiSummaryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "AI 配置")
@RequestMapping("/sys/ai")
@RequiredArgsConstructor
public class AiController {

    private final ArticleAiSummaryService articleAiSummaryService;

    @PostMapping("/test")
    @SaCheckPermission("sys:config:update")
    @ApiOperation(value = "测试 AI 接口连接")
    public Result<String> test() {
        return Result.success(articleAiSummaryService.testConnection());
    }
}

