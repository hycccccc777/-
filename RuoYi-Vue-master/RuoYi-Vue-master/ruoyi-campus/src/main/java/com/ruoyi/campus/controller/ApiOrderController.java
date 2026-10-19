package com.ruoyi.campus.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.campus.domain.CtOrderCreateBody;
import com.ruoyi.campus.service.ICtOrderService;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 面向 /api/v1 前端的订单接口（需登录）
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1")
public class ApiOrderController
{
    @Autowired
    private ICtOrderService orderService;

    /**
     * 下单：按卖家拆单，支持幂等键（请求体 idempotencyKey 或 Idempotency-Key 头）
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody(required = false) CtOrderCreateBody body,
            @RequestParam(value = "idempotencyKey", required = false) String keyParam)
    {
        if (body == null)
        {
            body = new CtOrderCreateBody();
        }
        if (isEmpty(body.getIdempotencyKey()) && !isEmpty(keyParam))
        {
            body.setIdempotencyKey(keyParam);
        }
        try
        {
            return ApiResp.ok(orderService.createOrder(body));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 我买到的订单
     */
    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> list()
    {
        return ApiResp.ok(orderService.listMyOrders(SecurityUtils.getUserId()));
    }

    /**
     * 我卖出的订单
     */
    @GetMapping("/orders/sold")
    public ResponseEntity<Map<String, Object>> listSold()
    {
        return ApiResp.ok(orderService.listMySales(SecurityUtils.getUserId()));
    }

    /**
     * 订单详情
     */
    @GetMapping("/orders/{orderNo}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable("orderNo") String orderNo)
    {
        try
        {
            return ApiResp.ok(orderService.orderDetail(SecurityUtils.getUserId(), orderNo));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 取消订单
     */
    @PutMapping("/orders/{orderNo}/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@PathVariable("orderNo") String orderNo,
            @RequestParam(value = "reason", required = false) String reason)
    {
        try
        {
            orderService.cancelOrder(SecurityUtils.getUserId(), orderNo, reason);
            return ApiResp.ok(null);
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 确认收货
     */
    @PutMapping("/orders/{orderNo}/confirm")
    public ResponseEntity<Map<String, Object>> confirm(@PathVariable("orderNo") String orderNo)
    {
        try
        {
            orderService.confirmOrder(SecurityUtils.getUserId(), orderNo);
            return ApiResp.ok(null);
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 卖家发货（仅待发货）
     */
    @PutMapping("/orders/{orderNo}/ship")
    public ResponseEntity<Map<String, Object>> ship(@PathVariable("orderNo") String orderNo)
    {
        try
        {
            orderService.shipOrder(SecurityUtils.getUserId(), orderNo);
            return ApiResp.ok(null);
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    private boolean isEmpty(String s)
    {
        return s == null || s.trim().isEmpty();
    }
}