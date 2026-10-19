package com.ruoyi.campus.contract;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 前端接口的 OpenAPI 分组。
 *
 * <p>只暴露 /api/v1/**：若依后台 /ruoyi/** 有 200+ 管理接口，
 * 混进同一份文档会让前端的类型生成把后台字段也带进来，既噪音又容易误用。
 * 分组后 Swagger UI 上会出现 "campus" 与 "default" 两个分组。
 *
 * <p>注意这里刻意<strong>不</strong>再定义 OpenAPI bean：
 * 若依的 SwaggerConfig 已经提供了全局 OpenAPI（自定义 bean 会在启动时
 * 因 "2 个 OpenAPI bean" 直接启动失败），而且它定义的 apikey 安全方案
 * （请求头 Authorization + Bearer）与前端实际用法完全一致，无需重复。
 * 文档标题等元信息在 SwaggerConfig#getApiInfo 中调整。
 *
 * @author campus
 */
@Configuration
public class CampusApiContractConfig
{
    private static final String GROUP = "campus";

    @Bean
    public GroupedOpenApi campusApi()
    {
        return GroupedOpenApi.builder()
                .group(GROUP)
                .pathsToMatch("/api/v1/**")
                .build();
    }
}