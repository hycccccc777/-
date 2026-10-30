package com.ruoyi.campus.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.common.annotation.Anonymous;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 公开配置接口：告诉前端当前环境启用了哪些能力，避免前端把「短信验证」
 * 或「线下面交」写死。
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "系统配置", description = "前端启动时读取的运行期能力开关")
public class ApiConfigController
{
    @Value("${campus.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${campus.payment.method:0}")
    private String payMethod;

    @Value("${campus.auth.register-enabled:true}")
    private boolean registerEnabled;

    @Anonymous
    @GetMapping("/config")
    @Operation(summary = "运行期配置", description = "短信开关、默认支付方式、注册开关")
    public ResponseEntity<Map<String, Object>> config()
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("smsEnabled", smsEnabled);
        data.put("payMethod", payMethod);
        data.put("registerEnabled", registerEnabled);
        return ApiResp.ok(data);
    }
}
