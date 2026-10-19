package com.ruoyi.campus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.ruoyi.campus.ratelimit.RateLimitInterceptor;

/**
 * 注册前端接口专用的 MVC 扩展。
 *
 * @author campus
 */
@Configuration
public class ApiWebMvcConfig implements WebMvcConfigurer
{
    private final RateLimitInterceptor rateLimitInterceptor;

    public ApiWebMvcConfig(RateLimitInterceptor rateLimitInterceptor)
    {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        // 只作用于 /api/v1/**：若依后台 /ruoyi/** 不限流，避免影响管理端批量操作
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/api/v1/**");
    }
}