package com.domerry.service.impl;

import com.domerry.service.TagService;
import com.domerry.vo.tag.TagListVo;
import com.domerry.mapper.SysTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final SysTagMapper sysTagMapper;

    @Override
    public List<TagListVo> getTagsApi() {
        return sysTagMapper.getTagsApi();
    }
}
