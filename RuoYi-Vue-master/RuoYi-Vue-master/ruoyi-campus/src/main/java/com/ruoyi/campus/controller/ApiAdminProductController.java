package com.ruoyi.campus.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.campus.domain.CtProductAuditBody;
import com.ruoyi.campus.domain.vo.CtProductVo;
import com.ruoyi.campus.service.ICtProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 商品审核（管理员）。
 *
 * <p>只对 admin 角色开放。上架前的人工审核是二手平台的必要环节：
 * 没有它，违规内容可以绕过任何发布时校验直接出现在商品列表里。
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "商品审核", description = "管理员审核待发布商品")
public class ApiAdminProductController
{
    @Autowired
    private ICtProductService productService;

    /**
     * 待审核商品列表（分页）。
     */
    @PreAuthorize("@ss.hasRole('admin')")
    @GetMapping("/products/pending")
    @Operation(summary = "待审核商品列表", description = "仅管理员；按发布时间倒序")
    public ResponseEntity<Map<String, Object>> pending(CtProductVo query,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size)
    {
        if (page < 1)
        {
            page = 1;
        }
        if (size < 1 || size > 100)
        {
            size = 20;
        }
        // 强制只看待审核，忽略调用方传入的 status，避免越权查看其它状态
        query.setStatus("0");
        PageHelper.startPage(page, size);
        try
        {
            List<CtProductVo> list = productService.selectCtProductVoList(query);
            PageInfo<CtProductVo> info = new PageInfo<>(list);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("total", info.getTotal());
            data.put("page", page);
            data.put("size", size);
            data.put("list", info.getList());
            return ApiResp.ok(data);
        }
        finally
        {
            PageHelper.clearPage();
        }
    }

    /**
     * 审核：通过则上架，驳回则记录原因。
     */
    @PreAuthorize("@ss.hasRole('admin')")
    @PostMapping("/products/{id}/audit")
    @Operation(summary = "审核商品", description = "仅管理员；approve=true 上架，false 驳回并记录原因")
    public ResponseEntity<Map<String, Object>> audit(@PathVariable("id") Long id,
            @RequestBody CtProductAuditBody body)
    {
        try
        {
            int rows = productService.auditProduct(id, body != null && body.isApprove(),
                    body == null ? null : body.getRemark());
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", String.valueOf(id));
            data.put("status", body != null && body.isApprove() ? "1" : "3");
            data.put("rows", rows);
            return ApiResp.ok(data);
        }
        catch (ApiException e)
        {
            return ApiResp.fail(e.getCode(), e.getMessage());
        }
    }
}
