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
import com.ruoyi.campus.domain.CtOrderItem;
import com.ruoyi.campus.service.ICtOrderItemService;

/**
 * 订单明细Controller
 * 
 * @author campus
 */
@RestController
@RequestMapping("/campus/orderItem")
public class CtOrderItemController extends BaseController
{
    @Autowired
    private ICtOrderItemService ctOrderItemService;

    @PreAuthorize("@ss.hasPermi('campus:orderItem:list')")
    @GetMapping("/list")
    public TableDataInfo list(CtOrderItem ctOrderItem)
    {
        startPage();
        List<CtOrderItem> list = ctOrderItemService.selectCtOrderItemList(ctOrderItem);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('campus:orderItem:export')")
    @Log(title = "订单明细", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CtOrderItem ctOrderItem)
    {
        List<CtOrderItem> list = ctOrderItemService.selectCtOrderItemList(ctOrderItem);
        ExcelUtil<CtOrderItem> util = new ExcelUtil<CtOrderItem>(CtOrderItem.class);
        util.exportExcel(response, list, "订单明细数据");
    }

    @PreAuthorize("@ss.hasPermi('campus:orderItem:query')")
    @GetMapping(value = "/{itemId}")
    public AjaxResult getInfo(@PathVariable("itemId") Long itemId)
    {
        return success(ctOrderItemService.selectCtOrderItemById(itemId));
    }

    @PreAuthorize("@ss.hasPermi('campus:orderItem:add')")
    @Log(title = "订单明细", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CtOrderItem ctOrderItem)
    {
        return toAjax(ctOrderItemService.insertCtOrderItem(ctOrderItem));
    }

    @PreAuthorize("@ss.hasPermi('campus:orderItem:edit')")
    @Log(title = "订单明细", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CtOrderItem ctOrderItem)
    {
        return toAjax(ctOrderItemService.updateCtOrderItem(ctOrderItem));
    }

    @PreAuthorize("@ss.hasPermi('campus:orderItem:remove')")
    @Log(title = "订单明细", businessType = BusinessType.DELETE)
    @DeleteMapping("/{itemIds}")
    public AjaxResult remove(@PathVariable Long[] itemIds)
    {
        return toAjax(ctOrderItemService.deleteCtOrderItemByIds(itemIds));
    }
}
