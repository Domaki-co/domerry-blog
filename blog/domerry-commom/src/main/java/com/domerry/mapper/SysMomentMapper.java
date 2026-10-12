package com.domerry.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.domerry.entity.SysMoment;
import com.domerry.vo.moment.MomentPageVo;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author: quequnlong
 * @date: 2025/2/5
 * @description:
 */
@Mapper
public interface SysMomentMapper extends BaseMapper<SysMoment> {


    IPage<MomentPageVo> selectPage(IPage<SysMoment> page);
}
