package com.domerry.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.domerry.mapper.SysMomentMapper;
import com.domerry.service.MomentService;
import com.domerry.utils.PageUtil;
import com.domerry.vo.moment.MomentPageVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author: quequnlong
 * @date: 2025/2/5
 * @description:
 */
@Service
@RequiredArgsConstructor
public class MomentServiceImpl implements MomentService {

    private final SysMomentMapper baseMapper;

    @Override
    public IPage<MomentPageVo> getMomentList() {
        return baseMapper.selectPage(PageUtil.getPage());
    }
}
