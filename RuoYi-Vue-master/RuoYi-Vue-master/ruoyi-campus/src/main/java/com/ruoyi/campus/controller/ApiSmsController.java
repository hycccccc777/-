package com.ruoyi.campus.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.campus.domain.CtSmsBody;
import com.ruoyi.campus.ratelimit.RateLimit;
import com.ruoyi.campus.sms.SmsCodeService;
import com.ruoyi.common.annotation.Anonymous;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 短信验证码接口。
 *
 * <p>匿名接口但限流更严：短信按条计费，被刷就是真金白银的损失。
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "短信验证码", description = "注册/登录/找回密码验证码")
public class ApiSmsController
{
    @Autowired
    private SmsCodeService smsCodeService;

    @Anonymous
    @RateLimit(permits = 3, seconds = 60, key = "sms", scope = RateLimit.Scope.IP)
    @PostMapping("/auth/sms/send")
    @Operation(summary = "发送验证码", description = "同一手机号有发送间隔与每日上限")
    public ResponseEntity<Map<String, Object>> send(@RequestBody CtSmsBody body)
    {
        String phone = body == null ? null : body.getPhone();
        if (phone == null || !phone.trim().matches("^1[3-9]\\d{9}$"))
        {
            return ApiResp.fail(400, "请输入有效的 11 位手机号");
        }
        try
        {
            smsCodeService.send(phone.trim(), body.getScene());
            return ApiResp.ok(null);
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }
}
