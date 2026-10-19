package com.ruoyi.campus.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.campus.domain.CtCartBody;
import com.ruoyi.campus.service.ICtCartItemService;
import com.ruoyi.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 面向 /api/v1 前端的购物车接口（需登录）
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "购物车", description = "加购、改数量、移除与列表")
public class ApiCartController
{
    @Autowired
    private ICtCartItemService cartService;

    /**
     * 我的购物车
     */
    @GetMapping("/cart")
    @Operation(summary = "我的购物车")
    public ResponseEntity<Map<String, Object>> list()
    {
        return ApiResp.ok(cartService.listWithProduct(SecurityUtils.getUserId()));
    }

    /**
     * 加入购物车
     */
    @PostMapping("/cart/items")
    @Operation(summary = "加购")
    public ResponseEntity<Map<String, Object>> add(@RequestBody CtCartBody body)
    {
        try
        {
            return ApiResp.ok(cartService.addItem(SecurityUtils.getUserId(), body.getProductId(), body.getQuantity()));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 修改数量（id 为购物车条目ID）
     */
    @PutMapping("/cart/items/{cartItemId}")
    @Operation(summary = "修改购物车数量")
    public ResponseEntity<Map<String, Object>> update(@PathVariable("cartItemId") Long cartItemId,
            @RequestBody CtCartBody body)
    {
        try
        {
            return ApiResp.ok(cartService.updateItem(SecurityUtils.getUserId(), cartItemId, body.getQuantity()));
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 移出购物车（id 为购物车条目ID）
     */
    @DeleteMapping("/cart/items/{cartItemId}")
    @Operation(summary = "从购物车移除")
    public ResponseEntity<Map<String, Object>> remove(@PathVariable("cartItemId") Long cartItemId)
    {
        try
        {
            cartService.removeItem(SecurityUtils.getUserId(), cartItemId);
            return ApiResp.ok(null);
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }
}