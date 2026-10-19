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
import com.ruoyi.campus.domain.CtAddress;
import com.ruoyi.campus.service.ICtAddressService;

/**
 * 收货地址Controller
 * 
 * @author campus
 */
@RestController
@RequestMapping("/campus/address")
public class CtAddressController extends BaseController
{
    @Autowired
    private ICtAddressService ctAddressService;

    @PreAuthorize("@ss.hasPermi('campus:address:list')")
    @GetMapping("/list")
    public TableDataInfo list(CtAddress ctAddress)
    {
        startPage();
        List<CtAddress> list = ctAddressService.selectCtAddressList(ctAddress);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('campus:address:export')")
    @Log(title = "收货地址", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CtAddress ctAddress)
    {
        List<CtAddress> list = ctAddressService.selectCtAddressList(ctAddress);
        ExcelUtil<CtAddress> util = new ExcelUtil<CtAddress>(CtAddress.class);
        util.exportExcel(response, list, "收货地址数据");
    }

    @PreAuthorize("@ss.hasPermi('campus:address:query')")
    @GetMapping(value = "/{addressId}")
    public AjaxResult getInfo(@PathVariable("addressId") Long addressId)
    {
        return success(ctAddressService.selectCtAddressById(addressId));
    }

    @PreAuthorize("@ss.hasPermi('campus:address:add')")
    @Log(title = "收货地址", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CtAddress ctAddress)
    {
        ctAddress.setCreateBy(getUsername());
        return toAjax(ctAddressService.insertCtAddress(ctAddress));
    }

    @PreAuthorize("@ss.hasPermi('campus:address:edit')")
    @Log(title = "收货地址", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CtAddress ctAddress)
    {
        ctAddress.setUpdateBy(getUsername());
        return toAjax(ctAddressService.updateCtAddress(ctAddress));
    }

    @PreAuthorize("@ss.hasPermi('campus:address:remove')")
    @Log(title = "收货地址", businessType = BusinessType.DELETE)
    @DeleteMapping("/{addressIds}")
    public AjaxResult remove(@PathVariable Long[] addressIds)
    {
        return toAjax(ctAddressService.deleteCtAddressByIds(addressIds));
    }
}
