package com.domerry.config;

import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// jasypt 3.x 默认算法已改为 PBEWITHHMACSHA512ANDAES_256，无法解开存量 ENC 密文（2.x 默认 PBEWITHMD5ANDDES 生成）；
// 必须注册自定义加密器并命名为 jasyptStringEncryptor（jasypt 3.x 仅按该名探测），详见 docs/升级方案-SpringBoot2.7与依赖漏洞治理.md
@Configuration
public class JasyptConfig {
    /**
     * 加解密盐值
     */
    @Value("${jasypt.encryptor.password}")
    private String password;

    @Bean("jasyptStringEncryptor")
    public StringEncryptor jasyptStringEncryptor() {
        return new MyStringEncryptor(password);
    }
}

