package com.domerry.controller.music;

import com.alibaba.fastjson2.JSONObject;
import com.domerry.annotation.AccessLimit;
import com.domerry.service.MusicService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 门户-音乐代理
 * <p>
 * 前端不再直连第三方音乐接口，改为调用本控制器；第三方地址与 apiKey 由后端从字典表读取。
 * 所有接口原样返回第三方响应体（不包 Result），失败时返回 {"code":500,"message":"..."}。
 * <p>
 * 音乐页对匿名访客开放，故不加 @SaCheckLogin；但代理消耗共享的第三方配额，
 * 因此每个接口按 IP 限流，具体额度见各方法上的 @AccessLimit。
 */
@RestController
@RequestMapping("/api/music")
@RequiredArgsConstructor
@Api(tags = "门户-音乐代理")
public class MusicController {

    private final MusicService musicService;

    /**
     * 搜索：用户主动触发，单次消耗一次上游搜索配额，给 20 次/分钟的余量足够连续换词
     */
    @AccessLimit(count = 20, time = 60)
    @GetMapping("/search")
    @ApiOperation(value = "搜索歌曲")
    public JSONObject search(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) Integer limit) {
        return musicService.search(keyword, limit);
    }

    /**
     * 歌单导入：低频操作，10 次/分钟已远超正常使用
     */
    @AccessLimit(count = 10, time = 60)
    @GetMapping("/playlist")
    @ApiOperation(value = "获取歌单")
    public JSONObject playlist(@RequestParam(required = false) String id) {
        return musicService.playlist(id);
    }

    /**
     * 播放地址：前端每加载一首歌调用一次，且与歌词并发调用；快速切歌场景需要更大余量
     */
    @AccessLimit(count = 60, time = 60)
    @GetMapping("/song")
    @ApiOperation(value = "获取歌曲播放地址")
    public JSONObject song(@RequestParam(required = false) String id,
                           @RequestParam(required = false) String level) {
        return musicService.song(id, level);
    }

    /**
     * 歌词：与播放地址成对调用，额度保持一致
     */
    @AccessLimit(count = 60, time = 60)
    @GetMapping("/lyric")
    @ApiOperation(value = "获取歌词")
    public JSONObject lyric(@RequestParam(required = false) String id) {
        return musicService.lyric(id);
    }
}
