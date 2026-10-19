package com.ruoyi.campus.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.campus.domain.CtRefundApplyBody;
import com.ruoyi.campus.service.ICtRefundService;
import com.ruoyi.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 面向 /api/v1 前端的退款/售后接口（需登录）
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "退款", description = "申请、审核与完成")
public class ApiRefundController
{
    @Autowired
    private ICtRefundService refundService;

    /**
     * 买家申请退款：订单转「退款中」
     */
    @PostMapping("/orders/{orderNo}/refund")
    @Operation(summary = "申请退款")
    public ResponseEntity<Map<String, Object>> apply(@PathVariable("orderNo") String orderNo,
            @Valid @RequestBody(required = false) CtRefundApplyBody body)
    {
        try
        {
            return ApiResp.ok(refundService.apply(SecurityUtils.getUserId(), orderNo, body == null ? null : body.getReason()));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 我发起的退款
     */
    @GetMapping("/refunds")
    @Operation(summary = "我发起的退款")
    public ResponseEntity<Map<String, Object>> listMine()
    {
        return ApiResp.ok(refundService.listMine(SecurityUtils.getUserId()));
    }

    /**
     * 我作为卖家需要处理的退款
     */
    @GetMapping("/refunds/incoming")
    @Operation(summary = "收到的退款申请", description = "卖家视角")
    public ResponseEntity<Map<String, Object>> listIncoming()
    {
        return ApiResp.ok(refundService.listIncoming(SecurityUtils.getUserId()));
    }

    /**
     * 卖家同意退款
     */
    @PostMapping("/refunds/{refundNo}/agree")
    @Operation(summary = "同意退款")
    public ResponseEntity<Map<String, Object>> agree(@PathVariable("refundNo") String refundNo)
    {
        try
        {
            return ApiResp.ok(refundService.agree(SecurityUtils.getUserId(), refundNo));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 卖家拒绝退款：订单回滚到申请前状态
     */
    @PostMapping("/refunds/{refundNo}/reject")
    @Operation(summary = "拒绝退款")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable("refundNo") String refundNo,
            @Valid @RequestBody(required = false) CtRefundApplyBody body)
    {
        try
        {
            return ApiResp.ok(refundService.reject(SecurityUtils.getUserId(), refundNo, body == null ? null : body.getRemark()));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 卖家确认退款到账：订单转「已退款」并回滚库存
     */
    @PostMapping("/refunds/{refundNo}/complete")
    @Operation(summary = "退款完成")
    public ResponseEntity<Map<String, Object>> complete(@PathVariable("refundNo") String refundNo)
    {
        try
        {
            return ApiResp.ok(refundService.complete(SecurityUtils.getUserId(), refundNo));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }
}