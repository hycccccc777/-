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

/**
 * 面向 /api/v1 前端的类目接口（公开）
 *
 * @author campus
 */
@Anonymous
@RestController
@RequestMapping("/api/v1")
public class ApiCategoryController
{
    @Autowired
    private ICtCategoryService categoryService;

    /**
     * 启用中的类目列表
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> list()
    {
        CtCategory query = new CtCategory();
        query.setStatus("0");
        List<CtCategory> list = categoryService.selectCtCategoryList(query);
        return ApiResp.ok(list);
    }
}
