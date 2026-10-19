package com.ruoyi.campus.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.campus.domain.CtCategory;
import com.ruoyi.campus.service.ICtCategoryService;

/**
 * 商品类目Controller
 * 
 * @author campus
 */
@RestController
@RequestMapping("/campus/category")
public class CtCategoryController extends BaseController
{
    @Autowired
    private ICtCategoryService ctCategoryService;

    @PreAuthorize("@ss.hasPermi('campus:category:list')")
    @GetMapping("/list")
    public TableDataInfo list(CtCategory ctCategory)
    {
        startPage();
        List<CtCategory> list = ctCategoryService.selectCtCategoryList(ctCategory);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('campus:category:export')")
    @Log(title = "商品类目", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CtCategory ctCategory)
    {
        List<CtCategory> list = ctCategoryService.selectCtCategoryList(ctCategory);
        ExcelUtil<CtCategory> util = new ExcelUtil<CtCategory>(CtCategory.class);
        util.exportExcel(response, list, "商品类目数据");
    }

    @PreAuthorize("@ss.hasPermi('campus:category:query')")
    @GetMapping(value = "/{categoryId}")
    public AjaxResult getInfo(@PathVariable("categoryId") Long categoryId)
    {
        return success(ctCategoryService.selectCtCategoryById(categoryId));
    }

    @PreAuthorize("@ss.hasPermi('campus:category:add')")
    @Log(title = "商品类目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CtCategory ctCategory)
    {
        ctCategory.setCreateBy(getUsername());
        return toAjax(ctCategoryService.insertCtCategory(ctCategory));
    }

    @PreAuthorize("@ss.hasPermi('campus:category:edit')")
    @Log(title = "商品类目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CtCategory ctCategory)
    {
        ctCategory.setUpdateBy(getUsername());
        return toAjax(ctCategoryService.updateCtCategory(ctCategory));
    }

    @PreAuthorize("@ss.hasPermi('campus:category:remove')")
    @Log(title = "商品类目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{categoryIds}")
    public AjaxResult remove(@PathVariable Long[] categoryIds)
    {
        return toAjax(ctCategoryService.deleteCtCategoryByIds(categoryIds));
    }
}
