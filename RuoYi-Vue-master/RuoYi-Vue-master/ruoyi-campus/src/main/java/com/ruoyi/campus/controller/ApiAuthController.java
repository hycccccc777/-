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
import com.ruoyi.campus.domain.CtRefreshBody;
import com.ruoyi.campus.ratelimit.RateLimit;
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
    /**
     * 登录限流：同一来源 1 分钟内最多 10 次。
     *
     * <p>阈值不设更严是有原因的——校园网/基站 NAT 下大量用户会共享同一个
     * 出口 IP，按 IP 卡得太死会把正常用户一起挡在门外，因此未登录场景
     * 选用相对宽松的阈值，靠「密码错误计数」再兜一层。
     */
    private static final int LOGIN_LIMIT_PER_MINUTE = 10;

    @Autowired
    private ICtAuthService authService;

    /**
     * 手机号 + 密码登录
     */
    @Anonymous
    @RateLimit(permits = 10, seconds = 60, key = "login", scope = RateLimit.Scope.IP)
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
     * 用刷新令牌换取新的访问令牌
     *
     * <p>无需登录即可调用（本身持有令牌），且限流更严：攻击者可以拿到
     * 别人的刷新令牌来反复试探，10 次/分钟足以拖垮 Redis。
     */
    @Anonymous
    @RateLimit(permits = 10, seconds = 60, key = "refresh", scope = RateLimit.Scope.IP)
    @PostMapping("/auth/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody CtRefreshBody body)
    {
        try
        {
            return ApiResp.ok(authService.refresh(body.getRefreshToken()));
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