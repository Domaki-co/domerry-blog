package com.domerry;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFileStorage
@EnableCaching
@EnableScheduling
@MapperScan("com.domerry.mapper")
public class DomerryAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(DomerryAdminApplication.class, args);
        System.out.println("系统启动成功！");
    }
}
