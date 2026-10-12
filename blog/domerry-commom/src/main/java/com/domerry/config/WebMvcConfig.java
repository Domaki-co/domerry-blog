package com.domerry.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.domerry.config.interceptor.ApiAccessLogInterceptor;
import com.domerry.entity.SysFileOss;
import com.domerry.enums.FileOssEnum;
import com.domerry.mapper.SysFileOssMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @author blue
 * @date 2022/3/10
 * @apiNote
 */
@Configuration
@EnableWebMvc
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SysFileOssMapper sysFileOssMapper;

    @Value("${cors.allowed-origins:http://localhost:3001,http://localhost:30001,https://your-domain.example,https://admin.your-domain.example}")
    private String allowedOrigins;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        SysFileOss sysFileOss = sysFileOssMapper.selectOne(new LambdaQueryWrapper<SysFileOss>()
                .eq(SysFileOss::getPlatform, FileOssEnum.LOCAL.getValue()));

        if (sysFileOss != null) {
            //本地存储升级版
            registry.addResourceHandler(sysFileOss.getPathPatterns())
                    .addResourceLocations("file:" + sysFileOss.getStoragePath());
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiAccessLogInterceptor());
    }

    /**
     * 注册跨域信息（与 MyCorsFilter 使用同一份 cors.allowed-origins 白名单）
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedHeaders("*")
                .allowedMethods("*")
                .maxAge(3600);
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }



}
