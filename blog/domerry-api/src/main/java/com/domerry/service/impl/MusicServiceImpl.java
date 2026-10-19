package com.domerry.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.domerry.common.Constants;
import com.domerry.entity.SysDictData;
import com.domerry.mapper.SysDictDataMapper;
import com.domerry.service.MusicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门户音乐代理服务
 * <p>
 * 第三方音乐接口地址、apiKey、超时时间均配置在字典表：dictType = music_api（label 存配置键，value 存配置值），
 * 后台修改字典即时生效，无需重启。密钥只存在于后端，前端与打包产物不再包含。
 * <p>
 * 上游响应体原样透传（不包 Result），保证前端既有的 code / data / data.songs / tracks / data.lrc 解析不变；
 * 任何上游失败都返回 {"code":500,"message":"..."} 且 HTTP 200，由前端既有错误分支处理。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MusicServiceImpl implements MusicService {

    private static final String DICT_TYPE_MUSIC_API = "music_api";

    private static final String UPSTREAM_SEARCH = "163_search";

    private static final String UPSTREAM_PLAYLIST = "163_playlist";

    private static final String UPSTREAM_MUSIC = "163_music";

    private static final String UPSTREAM_LYRIC = "163_lyric";

    private static final int DEFAULT_LIMIT = 30;

    private static final int DEFAULT_TIMEOUT = 10000;

    private static final int ERROR_CODE = 500;

    private final SysDictDataMapper sysDictDataMapper;

    @Override
    public JSONObject search(String keyword, Integer limit) {
        if (StringUtils.isBlank(keyword)) {
            return error("搜索关键词不能为空");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("keyword", keyword.trim());
        params.put("limit", limit == null || limit <= 0 ? DEFAULT_LIMIT : limit);
        return requestUpstream(UPSTREAM_SEARCH, params);
    }

    @Override
    public JSONObject playlist(String id) {
        if (StringUtils.isBlank(id)) {
            return error("歌单 id 不能为空");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", id.trim());
        return requestUpstream(UPSTREAM_PLAYLIST, params);
    }

    @Override
    public JSONObject song(String id, String level) {
        if (StringUtils.isBlank(id)) {
            return error("歌曲 id 不能为空");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", id.trim());
        if (StringUtils.isNotBlank(level)) {
            params.put("level", level.trim());
        }
        return requestUpstream(UPSTREAM_MUSIC, params);
    }

    @Override
    public JSONObject lyric(String id) {
        if (StringUtils.isBlank(id)) {
            return error("歌曲 id 不能为空");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", id.trim());
        return requestUpstream(UPSTREAM_LYRIC, params);
    }

    /**
     * 从字典表读取音乐接口配置（label -> value）
     */
    private Map<String, String> getMusicApiConfig() {
        Map<String, String> config = new HashMap<>();
        List<SysDictData> dictDataList = sysDictDataMapper.selectDataByDictType(DICT_TYPE_MUSIC_API);
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
     * 调用第三方音乐接口，成功原样返回响应体，失败返回统一错误体
     * <p>
     * 日志只记录上游路径，不记录 apiKey；返回给前端的 message 也不包含地址与密钥。
     */
    private JSONObject requestUpstream(String path, Map<String, Object> params) {
        Map<String, String> config = getMusicApiConfig();
        String url = config.get("url");
        String apiKey = config.get("apiKey");
        if (StringUtils.isAnyBlank(url, apiKey)) {
            log.error("音乐接口代理失败：字典 {} 缺少 url 或 apiKey 配置", DICT_TYPE_MUSIC_API);
            return error("音乐服务未配置，请联系管理员");
        }

        Map<String, Object> form = new LinkedHashMap<>();
        form.put("apikey", apiKey);
        form.putAll(params);

        try {
            HttpResponse response = HttpRequest.get(StringUtils.removeEnd(url.trim(), "/") + "/" + path)
                    .form(form)
                    .timeout(parseTimeout(config.get("timeout")))
                    .execute();
            int status = response.getStatus();
            String body = response.body();
            if (status != 200) {
                log.error("音乐接口代理失败：upstream={}, httpStatus={}", path, status);
                return error("音乐服务暂时不可用，请稍后重试");
            }
            if (StringUtils.isBlank(body)) {
                log.error("音乐接口代理失败：upstream={}, 响应体为空", path);
                return error("音乐服务返回数据异常");
            }
            JSONObject resp = JSON.parseObject(body.trim());
            if (resp == null) {
                log.error("音乐接口代理失败：upstream={}, 响应体解析为空", path);
                return error("音乐服务返回数据异常");
            }
            return resp;
        } catch (Exception e) {
            // 不打印原始异常：Hutool 在 URL 拼接失败时会把含 apikey 的完整地址写进异常信息，
            // 直接 log.error(..., e) 会把密钥带进日志；这里只记录异常类型与脱敏后的信息。
            log.error("音乐接口代理异常：upstream={}, type={}, error={}", path, e.getClass().getSimpleName(),
                    sanitize(e.getMessage(), apiKey));
            return error("音乐服务调用失败，请稍后重试");
        }
    }

    /**
     * 脱敏：把配置里的 apiKey 从日志文本中抹掉，避免密钥经由异常信息泄漏
     */
    private String sanitize(String text, String apiKey) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        if (StringUtils.isBlank(apiKey)) {
            return text;
        }
        return text.replace(apiKey, "***");
    }

    /**
     * 统一错误体：HTTP 200 + {"code":500,"message":"..."}
     */
    private JSONObject error(String message) {
        JSONObject result = new JSONObject();
        result.put("code", ERROR_CODE);
        result.put("message", message);
        return result;
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
