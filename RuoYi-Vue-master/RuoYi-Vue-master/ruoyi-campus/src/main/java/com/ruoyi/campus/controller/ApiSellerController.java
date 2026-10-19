package com.ruoyi.campus.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.campus.domain.CtProduct;
import com.ruoyi.campus.service.ICtProductService;
import com.ruoyi.common.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 面向 /api/v1 前端的卖家中心接口（需登录）
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "卖家中心", description = "我发布的商品管理")
public class ApiSellerController
{
    @Autowired
    private ICtProductService productService;

    /**
     * 我发布的商品
     */
    @GetMapping("/my/products")
    @Operation(summary = "我发布的商品", description = "可按状态过滤")
    public ResponseEntity<Map<String, Object>> myProducts(@RequestParam(value = "status", required = false) String status)
    {
        return ApiResp.ok(productService.selectMyProducts(SecurityUtils.getUserId(), status));
    }

    /**
     * 编辑我发布的商品
     */
    @PutMapping("/my/products/{productId}")
    @Operation(summary = "编辑商品")
    public ResponseEntity<Map<String, Object>> update(@PathVariable("productId") Long productId,
            @RequestBody CtProduct patch)
    {
        try
        {
            productService.updateMyProduct(SecurityUtils.getUserId(), productId, patch);
            return ApiResp.ok(null);
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 上架 / 下架
     */
    @PutMapping("/my/products/{productId}/status")
    @Operation(summary = "商品上/下架")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable("productId") Long productId,
            @RequestParam("status") String status)
    {
        try
        {
            productService.updateMyProductStatus(SecurityUtils.getUserId(), productId, status);
            return ApiResp.ok(null);
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }

    /**
     * 删除我发布的商品
     */
    @DeleteMapping("/my/products/{productId}")
    @Operation(summary = "删除商品")
    public ResponseEntity<Map<String, Object>> remove(@PathVariable("productId") Long productId)
    {
        try
        {
            productService.removeMyProduct(SecurityUtils.getUserId(), productId);
            return ApiResp.ok(null);
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }
}