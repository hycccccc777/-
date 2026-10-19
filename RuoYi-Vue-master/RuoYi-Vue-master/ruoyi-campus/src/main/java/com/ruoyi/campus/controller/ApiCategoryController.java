package com.ruoyi.campus.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.campus.domain.CtCategory;
import com.ruoyi.campus.service.ICtCategoryService;
import com.ruoyi.common.annotation.Anonymous;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 面向 /api/v1 前端的类目接口（公开）
 *
 * @author campus
 */
@Anonymous
@RestController
@RequestMapping("/api/v1")
@Tag(name = "类目", description = "启用中类目（公开）")
public class ApiCategoryController
{
    @Autowired
    private ICtCategoryService categoryService;

    /**
     * 启用中的类目列表
     */
    @GetMapping("/categories")
    @Operation(summary = "启用中的类目列表", description = "公开接口，无需登录")
    public ResponseEntity<Map<String, Object>> list()
    {
        CtCategory query = new CtCategory();
        query.setStatus("0");
        List<CtCategory> list = categoryService.selectCtCategoryList(query);
        return ApiResp.ok(list);
    }
}
