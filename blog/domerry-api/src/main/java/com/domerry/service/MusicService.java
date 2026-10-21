package com.domerry.service;

import com.alibaba.fastjson2.JSONObject;

/**
 * 门户音乐代理服务
 * <p>
 * 前端不再直连第三方音乐接口，统一由本服务代理转发，第三方地址与 apiKey 只存在于后端字典表。
 * 所有方法均原样返回第三方响应体，失败时返回 {"code":500,"message":"..."}。
 */
public interface MusicService {

    /**
     * 搜索歌曲
     *
     * @param keyword 搜索关键词
     * @param limit   返回条数，为空时默认 30
     * @return 第三方响应体原样
     */
    JSONObject search(String keyword, Integer limit);

    /**
     * 获取歌单详情
     *
     * @param id 歌单 id
     * @return 第三方响应体原样
     */
    JSONObject playlist(String id);

    /**
     * 获取歌曲播放地址
     *
     * @param id    歌曲 id
     * @param level 音质等级
     * @return 第三方响应体原样
     */
    JSONObject song(String id, String level);

    /**
     * 获取歌词
     *
     * @param id 歌曲 id
     * @return 第三方响应体原样
     */
    JSONObject lyric(String id);
}
