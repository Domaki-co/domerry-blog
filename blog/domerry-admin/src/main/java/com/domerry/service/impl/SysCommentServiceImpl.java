package com.domerry.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domerry.entity.SysComment;
import com.domerry.mapper.SysCommentMapper;
import com.domerry.service.SysCommentService;
import com.domerry.utils.PageUtil;
import com.domerry.vo.comment.SysCommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author: quequnlong
 * @date: 2025/1/2
 * @description:
 */
@Service
@RequiredArgsConstructor
public class SysCommentServiceImpl extends ServiceImpl<SysCommentMapper,SysComment> implements SysCommentService {

    @Override
    public Page<SysCommentVO> selectList() {
        return baseMapper.selectPage(PageUtil.getPage());
    }
}
