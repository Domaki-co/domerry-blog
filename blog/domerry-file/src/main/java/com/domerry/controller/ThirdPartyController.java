package com.domerry.controller;

import com.domerry.common.Result;
import com.domerry.service.ThirdPartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/third")
@Tag(name = "第三方")
@RequiredArgsConstructor
@Slf4j
public class ThirdPartyController {

    private final ThirdPartyService thirdPartyService;

    @GetMapping("/getSongList")
    @Operation(summary = "获取歌单列表")
    public Result getSongList() {
        List<Map<String, Object>> list = thirdPartyService.getSongList();
        return Result.success(list);
    }

}
