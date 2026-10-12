package com.domerry.service;

import com.domerry.vo.tag.TagListVo;

import java.util.List;

public interface TagService {

    /**
     * 获取标签列表
     * @return
     */
    List<TagListVo> getTagsApi();

}
