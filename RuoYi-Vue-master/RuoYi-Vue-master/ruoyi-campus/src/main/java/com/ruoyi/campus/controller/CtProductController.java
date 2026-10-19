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
import com.ruoyi.campus.domain.CtProduct;
import com.ruoyi.campus.service.ICtProductService;

/**
 * 商品Controller
 * 
 * @author campus
 */
@RestController
@RequestMapping("/campus/product")
public class CtProductController extends BaseController
{
    @Autowired
    private ICtProductService ctProductService;

    @PreAuthorize("@ss.hasPermi('campus:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(CtProduct ctProduct)
    {
        startPage();
        List<CtProduct> list = ctProductService.selectCtProductList(ctProduct);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('campus:product:export')")
    @Log(title = "商品", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CtProduct ctProduct)
    {
        List<CtProduct> list = ctProductService.selectCtProductList(ctProduct);
        ExcelUtil<CtProduct> util = new ExcelUtil<CtProduct>(CtProduct.class);
        util.exportExcel(response, list, "商品数据");
    }

    @PreAuthorize("@ss.hasPermi('campus:product:query')")
    @GetMapping(value = "/{productId}")
    public AjaxResult getInfo(@PathVariable("productId") Long productId)
    {
        return success(ctProductService.selectCtProductById(productId));
    }

    @PreAuthorize("@ss.hasPermi('campus:product:add')")
    @Log(title = "商品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CtProduct ctProduct)
    {
        ctProduct.setCreateBy(getUsername());
        return toAjax(ctProductService.insertCtProduct(ctProduct));
    }

    @PreAuthorize("@ss.hasPermi('campus:product:edit')")
    @Log(title = "商品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CtProduct ctProduct)
    {
        ctProduct.setUpdateBy(getUsername());
        return toAjax(ctProductService.updateCtProduct(ctProduct));
    }

    @PreAuthorize("@ss.hasPermi('campus:product:remove')")
    @Log(title = "商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{productIds}")
    public AjaxResult remove(@PathVariable Long[] productIds)
    {
        return toAjax(ctProductService.deleteCtProductByIds(productIds));
    }
}
