package com.domerry.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 第三方歌曲列表接口响应 VO（open-api/v1/songs）
 */
@Data
public class OpenSongRespVo {

    private Integer id;

    //歌名
    private String songName;

    //歌手
    private String artist;

    //播放地址
    private String songUrl;

    //歌词
    private String lrc;

    //封面图
    private String pic;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
