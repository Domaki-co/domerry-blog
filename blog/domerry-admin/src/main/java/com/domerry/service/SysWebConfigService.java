package com.domerry.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domerry.entity.SysWebConfig;

public interface SysWebConfigService extends IService<SysWebConfig> {

    void update(SysWebConfig sysWebConfig);
}
