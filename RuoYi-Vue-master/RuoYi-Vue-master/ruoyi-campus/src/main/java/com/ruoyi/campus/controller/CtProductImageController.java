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
import com.ruoyi.campus.domain.CtProductImage;
import com.ruoyi.campus.service.ICtProductImageService;

/**
 * 商品图片Controller
 * 
 * @author campus
 */
@RestController
@RequestMapping("/campus/productImage")
public class CtProductImageController extends BaseController
{
    @Autowired
    private ICtProductImageService ctProductImageService;

    @PreAuthorize("@ss.hasPermi('campus:productImage:list')")
    @GetMapping("/list")
    public TableDataInfo list(CtProductImage ctProductImage)
    {
        startPage();
        List<CtProductImage> list = ctProductImageService.selectCtProductImageList(ctProductImage);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('campus:productImage:export')")
    @Log(title = "商品图片", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CtProductImage ctProductImage)
    {
        List<CtProductImage> list = ctProductImageService.selectCtProductImageList(ctProductImage);
        ExcelUtil<CtProductImage> util = new ExcelUtil<CtProductImage>(CtProductImage.class);
        util.exportExcel(response, list, "商品图片数据");
    }

    @PreAuthorize("@ss.hasPermi('campus:productImage:query')")
    @GetMapping(value = "/{imageId}")
    public AjaxResult getInfo(@PathVariable("imageId") Long imageId)
    {
        return success(ctProductImageService.selectCtProductImageById(imageId));
    }

    @PreAuthorize("@ss.hasPermi('campus:productImage:add')")
    @Log(title = "商品图片", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CtProductImage ctProductImage)
    {
        return toAjax(ctProductImageService.insertCtProductImage(ctProductImage));
    }

    @PreAuthorize("@ss.hasPermi('campus:productImage:edit')")
    @Log(title = "商品图片", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CtProductImage ctProductImage)
    {
        return toAjax(ctProductImageService.updateCtProductImage(ctProductImage));
    }

    @PreAuthorize("@ss.hasPermi('campus:productImage:remove')")
    @Log(title = "商品图片", businessType = BusinessType.DELETE)
    @DeleteMapping("/{imageIds}")
    public AjaxResult remove(@PathVariable Long[] imageIds)
    {
        return toAjax(ctProductImageService.deleteCtProductImageByIds(imageIds));
    }
}
