package com.ruoyi.campus.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.common.annotation.Anonymous;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 面向 /api/v1 前端的系统接口（公开）
 *
 * @author campus
 */
@Tag(name = "系统", description = "探活等与业务无关的基础接口")
@Anonymous
@RestController
@RequestMapping("/api/v1")
public class ApiSystemController
{
    /**
     * 探活接口
     *
     * <p>移动端弱网下最常见的问题是「不知道后端是不通还是自己没网」。
     * 这个接口让前端在登录页就能判断后端可达性，避免用户反复点登录按钮。
     *
     * <p>只返回最少的健康信息：接口一旦对外开放，就不能顺带暴露版本号、
     * 数据库地址这类可被利用的信息。
     */

    @GetMapping("/system/health")
    @Operation(summary = "后端探活", description = "公开接口；用于前端判断后端是否可达，不返回任何环境敏感信息")
    public ResponseEntity<Map<String, Object>> health()
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "ok");
        data.put("serverTime", System.currentTimeMillis());
        return ApiResp.ok(data);
    }
}