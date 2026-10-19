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
import com.ruoyi.campus.domain.CtCartItem;
import com.ruoyi.campus.service.ICtCartItemService;

/**
 * 购物车条目Controller
 * 
 * @author campus
 */
@RestController
@RequestMapping("/campus/cartItem")
public class CtCartItemController extends BaseController
{
    @Autowired
    private ICtCartItemService ctCartItemService;

    @PreAuthorize("@ss.hasPermi('campus:cartItem:list')")
    @GetMapping("/list")
    public TableDataInfo list(CtCartItem ctCartItem)
    {
        startPage();
        List<CtCartItem> list = ctCartItemService.selectCtCartItemList(ctCartItem);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('campus:cartItem:export')")
    @Log(title = "购物车条目", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CtCartItem ctCartItem)
    {
        List<CtCartItem> list = ctCartItemService.selectCtCartItemList(ctCartItem);
        ExcelUtil<CtCartItem> util = new ExcelUtil<CtCartItem>(CtCartItem.class);
        util.exportExcel(response, list, "购物车条目数据");
    }

    @PreAuthorize("@ss.hasPermi('campus:cartItem:query')")
    @GetMapping(value = "/{cartItemId}")
    public AjaxResult getInfo(@PathVariable("cartItemId") Long cartItemId)
    {
        return success(ctCartItemService.selectCtCartItemById(cartItemId));
    }

    @PreAuthorize("@ss.hasPermi('campus:cartItem:add')")
    @Log(title = "购物车条目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CtCartItem ctCartItem)
    {
        return toAjax(ctCartItemService.insertCtCartItem(ctCartItem));
    }

    @PreAuthorize("@ss.hasPermi('campus:cartItem:edit')")
    @Log(title = "购物车条目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CtCartItem ctCartItem)
    {
        return toAjax(ctCartItemService.updateCtCartItem(ctCartItem));
    }

    @PreAuthorize("@ss.hasPermi('campus:cartItem:remove')")
    @Log(title = "购物车条目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{cartItemIds}")
    public AjaxResult remove(@PathVariable Long[] cartItemIds)
    {
        return toAjax(ctCartItemService.deleteCtCartItemByIds(cartItemIds));
    }
}
