package com.domerry.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.domerry.common.Constants;
import com.domerry.entity.OpenSongRespVo;
import com.domerry.entity.SysDictData;
import com.domerry.mapper.SysDictDataMapper;
import com.domerry.service.ThirdPartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 第三方接口服务
 * <p>
 * 歌单接口地址、apiKey、超时时间均配置在字典表：dictType = third_song_api（label 存配置键，value 存配置值），
 * 后台修改字典即时生效，无需重启。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ThirdPartyServiceImpl implements ThirdPartyService {

    private static final String DICT_TYPE_THIRD_SONG_API = "third_song_api";

    private static final String HEADER_API_KEY = "X-Api-Key";

    private static final int DEFAULT_TIMEOUT = 10000;

    private final SysDictDataMapper sysDictDataMapper;

    @Override
    public List<Map<String, Object>> getSongList() {
        Map<String, String> config = getSongApiConfig();
        String url = config.get("url");
        String apiKey = config.get("apiKey");
        if (StringUtils.isAnyBlank(url, apiKey)) {
            log.error("获取歌单失败：字典 {} 缺少 url 或 apiKey 配置", DICT_TYPE_THIRD_SONG_API);
            return Collections.emptyList();
        }
        String body = requestSongApi(url, apiKey, parseTimeout(config.get("timeout")));
        if (StringUtils.isBlank(body)) {
            return Collections.emptyList();
        }
        List<OpenSongRespVo> songs = parseSongs(body, url);
        if (songs == null || songs.isEmpty()) {
            return Collections.emptyList();
        }
        return songs.stream().map(this::toFrontMap).collect(Collectors.toList());
    }

    /**
     * 从字典表读取歌单接口配置（label -> value）
     */
    private Map<String, String> getSongApiConfig() {
        Map<String, String> config = new HashMap<>();
        List<SysDictData> dictDataList = sysDictDataMapper.selectDataByDictType(DICT_TYPE_THIRD_SONG_API);
        if (dictDataList == null) {
            return config;
        }
        for (SysDictData dictData : dictDataList) {
            if (dictData.getStatus() != null && dictData.getStatus() == Constants.YES) {
                config.put(dictData.getLabel(), dictData.getValue());
            }
        }
        return config;
    }

    /**
     * 调用第三方歌曲接口，失败返回 null
     */
    private String requestSongApi(String url, String apiKey, int timeout) {
        try {
            HttpResponse response = HttpRequest.get(url)
                    .header(HEADER_API_KEY, apiKey)
                    .timeout(timeout)
                    .execute();
            int status = response.getStatus();
            String body = response.body();
            if (status != 200) {
                log.error("第三方歌曲接口调用失败：url={}, httpStatus={}", url, status);
                return null;
            }
            return body;
        } catch (Exception e) {
            log.error("第三方歌曲接口调用异常：url={}", url, e);
            return null;
        }
    }

    /**
     * 解析响应，兼容 {code:0, msg, data:[...]} 包装结构和直接返回数组两种格式；解析失败返回 null
     */
    private List<OpenSongRespVo> parseSongs(String body, String url) {
        try {
            String trimmed = body.trim();
            if (trimmed.startsWith("[")) {
                return JSON.parseArray(trimmed, OpenSongRespVo.class);
            }
            JSONObject resp = JSON.parseObject(trimmed);
            Integer code = resp.getInteger("code"); // code为0表示成功
            if (code == null || code != Constants.NO) {
                log.error("第三方歌曲接口返回失败：url={}, code={}, msg={}", url, code, resp.getString("msg"));
                return null;
            }
            JSONArray data = resp.getJSONArray("data");
            if (data == null) {
                log.error("第三方歌曲接口返回 data 为空：url={}", url);
                return Collections.emptyList();
            }
            return data.toJavaList(OpenSongRespVo.class);
        } catch (Exception e) {
            log.error("第三方歌曲接口响应解析失败：url={}, body={}", url, body, e);
            return null;
        }
    }

    /**
     * 映射为前端播放器（player.vue）期望的字段：name / url / artist / lrc / cover
     */
    private Map<String, Object> toFrontMap(OpenSongRespVo song) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", song.getSongName());
        map.put("url", song.getSongUrl());
        map.put("artist", song.getArtist());
        map.put("lrc", song.getLrc());
        map.put("cover", song.getPic());
        return map;
    }

    private int parseTimeout(String timeout) {
        if (StringUtils.isNotBlank(timeout)) {
            try {
                return Math.max(1000, Integer.parseInt(timeout.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_TIMEOUT;
    }
}
