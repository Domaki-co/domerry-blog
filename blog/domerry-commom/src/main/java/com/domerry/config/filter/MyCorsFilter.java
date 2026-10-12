package com.domerry.config.filter;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 跨域过滤器
 * 只对白名单内的 Origin 回写 Access-Control-Allow-Origin，白名单见 application.yml 的 cors.allowed-origins
 * @author blue
 * @date 2022/3/11
 * @apiNote
 */
@Component
public class MyCorsFilter implements Filter {

    @Value("${cors.allowed-origins:http://localhost:3001,http://localhost:30001,https://your-domain.example,https://admin.your-domain.example}")
    private String allowedOrigins;

    @Override
    public void init(javax.servlet.FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        String origin = httpRequest.getHeader("Origin");
        if (origin != null && isAllowedOrigin(origin)) {
            httpResponse.setHeader("Access-Control-Allow-Origin", origin);
            httpResponse.setHeader("Vary", "Origin");
        }
        httpResponse.setHeader("Access-Control-Allow-Headers", "Authorization,Content-Type,X-Requested-With,token");
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        httpResponse.addHeader("Access-Control-Max-Age", "3600");
        if (httpRequest.getMethod().equals("OPTIONS")) {
            httpResponse.setStatus(200);
            return;
        }
        filterChain.doFilter(servletRequest, httpResponse);
    }

    private boolean isAllowedOrigin(String origin) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return false;
        }
        for (String allowed : allowedOrigins.split(",")) {
            if (allowed.trim().equals(origin)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
