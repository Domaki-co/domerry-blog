package com.domerry.config.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    // 注册拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，定义详细的拦截路由
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/logout",
                        "/auth/verify",
                        "/auth/exchangeCode",   // 第三方登录换取 token：此时前端还没有 token
                        "/swagger-ui/**",          // knife4j接口文档
                        "/webjars/**",        // knife4j相关资源
                        "/v3/api-docs/**",     // openapi接口文档
                        "/doc.html",     // openapi接口文档
                        "/favicon.ico",     // openapi接口文档
                        "/swagger-resources",
                        "/api/**",
                        "/sys/web/config",
                        "/wechat/**",
                        "/localFile/**",
                        "/third/**"
                );

        // 注解校验和上面那个 auth 回调在同一个 preHandle 里，被 excludePathPatterns 放行的路径
        // 连 @SaCheckLogin 都不会触发；所以留言提交单独挂一个只做注解校验的拦截器
        // （无参构造 = 空 auth 回调 + 注解校验），它不参与上面的排除名单
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/api/message/add");
    }
}
