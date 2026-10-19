package com.ruoyi.campus.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.campus.domain.CtLoginBody;
import com.ruoyi.campus.service.ICtAuthService;
import com.ruoyi.common.annotation.Anonymous;

/**
 * 面向 /api/v1 前端的认证接口（手机号 + 密码）
 * 
 * @author campus
 */
@RestController
@RequestMapping("/api/v1")
public class ApiAuthController
{
    @Autowired
    private ICtAuthService authService;

    /**
     * 手机号 + 密码登录
     */
    @Anonymous
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody CtLoginBody body)
    {
        try
        {
            return ApiResp.ok(authService.login(body));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 退出登录
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> logout()
    {
        authService.logout();
        return ApiResp.ok(null);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/user/info")
    public ResponseEntity<Map<String, Object>> userInfo()
    {
        try
        {
            return ApiResp.ok(authService.currentUser());
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }
}
