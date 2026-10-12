package com.domerry.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.domerry.entity.SysJobLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysJobLogMapper extends BaseMapper<SysJobLog> {
    
    void cleanJobLog();
} 