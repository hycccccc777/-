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
import com.ruoyi.campus.domain.CtOrder;
import com.ruoyi.campus.service.ICtOrderService;

/**
 * 订单Controller
 * 
 * @author campus
 */
@RestController
@RequestMapping("/campus/order")
public class CtOrderController extends BaseController
{
    @Autowired
    private ICtOrderService ctOrderService;

    @PreAuthorize("@ss.hasPermi('campus:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(CtOrder ctOrder)
    {
        startPage();
        List<CtOrder> list = ctOrderService.selectCtOrderList(ctOrder);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('campus:order:export')")
    @Log(title = "订单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CtOrder ctOrder)
    {
        List<CtOrder> list = ctOrderService.selectCtOrderList(ctOrder);
        ExcelUtil<CtOrder> util = new ExcelUtil<CtOrder>(CtOrder.class);
        util.exportExcel(response, list, "订单数据");
    }

    @PreAuthorize("@ss.hasPermi('campus:order:query')")
    @GetMapping(value = "/{orderId}")
    public AjaxResult getInfo(@PathVariable("orderId") Long orderId)
    {
        return success(ctOrderService.selectCtOrderById(orderId));
    }

    @PreAuthorize("@ss.hasPermi('campus:order:add')")
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CtOrder ctOrder)
    {
        ctOrder.setCreateBy(getUsername());
        return toAjax(ctOrderService.insertCtOrder(ctOrder));
    }

    @PreAuthorize("@ss.hasPermi('campus:order:edit')")
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CtOrder ctOrder)
    {
        ctOrder.setUpdateBy(getUsername());
        return toAjax(ctOrderService.updateCtOrder(ctOrder));
    }

    @PreAuthorize("@ss.hasPermi('campus:order:remove')")
    @Log(title = "订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orderIds}")
    public AjaxResult remove(@PathVariable Long[] orderIds)
    {
        return toAjax(ctOrderService.deleteCtOrderByIds(orderIds));
    }
}
