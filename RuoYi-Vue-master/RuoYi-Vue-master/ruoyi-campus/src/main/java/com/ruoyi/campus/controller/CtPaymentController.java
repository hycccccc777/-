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
import com.ruoyi.campus.domain.CtPayment;
import com.ruoyi.campus.service.ICtPaymentService;

/**
 * 支付单Controller
 * 
 * @author campus
 */
@RestController
@RequestMapping("/campus/payment")
public class CtPaymentController extends BaseController
{
    @Autowired
    private ICtPaymentService ctPaymentService;

    @PreAuthorize("@ss.hasPermi('campus:payment:list')")
    @GetMapping("/list")
    public TableDataInfo list(CtPayment ctPayment)
    {
        startPage();
        List<CtPayment> list = ctPaymentService.selectCtPaymentList(ctPayment);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('campus:payment:export')")
    @Log(title = "支付单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CtPayment ctPayment)
    {
        List<CtPayment> list = ctPaymentService.selectCtPaymentList(ctPayment);
        ExcelUtil<CtPayment> util = new ExcelUtil<CtPayment>(CtPayment.class);
        util.exportExcel(response, list, "支付单数据");
    }

    @PreAuthorize("@ss.hasPermi('campus:payment:query')")
    @GetMapping(value = "/{paymentId}")
    public AjaxResult getInfo(@PathVariable("paymentId") Long paymentId)
    {
        return success(ctPaymentService.selectCtPaymentById(paymentId));
    }

    @PreAuthorize("@ss.hasPermi('campus:payment:add')")
    @Log(title = "支付单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CtPayment ctPayment)
    {
        return toAjax(ctPaymentService.insertCtPayment(ctPayment));
    }

    @PreAuthorize("@ss.hasPermi('campus:payment:edit')")
    @Log(title = "支付单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CtPayment ctPayment)
    {
        return toAjax(ctPaymentService.updateCtPayment(ctPayment));
    }

    @PreAuthorize("@ss.hasPermi('campus:payment:remove')")
    @Log(title = "支付单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{paymentIds}")
    public AjaxResult remove(@PathVariable Long[] paymentIds)
    {
        return toAjax(ctPaymentService.deleteCtPaymentByIds(paymentIds));
    }
}
