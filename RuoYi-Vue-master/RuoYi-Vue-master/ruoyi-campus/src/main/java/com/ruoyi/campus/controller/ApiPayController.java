package com.ruoyi.campus.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.campus.domain.CtPayBody;
import com.ruoyi.campus.service.ICtPaymentService;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 面向 /api/v1 前端的支付接口
 *
 * <p>渠道选择由后端的 PaymentGatewayRegistry 分发，前端不需要知道当前
 * 跑的是沙箱网关还是真实网关。
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "支付", description = "建单、查状态、确认与网关回调")
public class ApiPayController
{
    @Autowired
    private ICtPaymentService paymentService;

    /**
     * 创建支付单
     */
    @PostMapping("/pay/create")
    @Operation(summary = "创建支付单", description = "返回支付网关参数；余额模式直接扣减")
    public ResponseEntity<Map<String, Object>> create(@RequestBody CtPayBody body)
    {
        try
        {
            return ApiResp.ok(paymentService.create(SecurityUtils.getUserId(), body.getOrderNo(), body.getPayMethod()));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/pay/{payNo}/status")
    @Operation(summary = "查询支付状态")
    public ResponseEntity<Map<String, Object>> status(@PathVariable("payNo") String payNo)
    {
        try
        {
            return ApiResp.ok(paymentService.getStatus(SecurityUtils.getUserId(), payNo));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 主动确认支付：仅沙箱/模拟网关支持，真实网关请走 /pay/notify
     */
    @PostMapping("/pay/{payNo}/confirm")
    @Operation(summary = "标记支付成功", description = "演示环境用；生产走异步回调")
    public ResponseEntity<Map<String, Object>> confirm(@PathVariable("payNo") String payNo)
    {
        try
        {
            return ApiResp.ok(paymentService.confirm(SecurityUtils.getUserId(), payNo));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 渠道异步回调（无需登录，由支付渠道服务端调用）。
     *
     * <p>必须携带签名，否则任何人伪造「支付成功」就能把订单刷成已付款。
     * 生产环境应走 @PermitAll + IP 白名单 + 渠道公钥验签。
     */
    @Anonymous
    @PostMapping("/pay/{payNo}/notify")
    @Operation(summary = "支付网关回调", description = "匿名接口，验签通过才处理")
    public ResponseEntity<Map<String, Object>> notify(@PathVariable("payNo") String payNo,
            @RequestParam(value = "sign", required = false) String sign)
    {
        try
        {
            return ApiResp.ok(paymentService.handleCallback(payNo, sign));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }
}