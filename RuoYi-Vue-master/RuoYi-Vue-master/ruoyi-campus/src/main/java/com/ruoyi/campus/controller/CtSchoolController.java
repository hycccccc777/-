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
import com.ruoyi.campus.domain.CtSchool;
import com.ruoyi.campus.service.ICtSchoolService;

/**
 * 学校Controller
 * 
 * @author campus
 */
@RestController
@RequestMapping("/campus/school")
public class CtSchoolController extends BaseController
{
    @Autowired
    private ICtSchoolService ctSchoolService;

    @PreAuthorize("@ss.hasPermi('campus:school:list')")
    @GetMapping("/list")
    public TableDataInfo list(CtSchool ctSchool)
    {
        startPage();
        List<CtSchool> list = ctSchoolService.selectCtSchoolList(ctSchool);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('campus:school:export')")
    @Log(title = "学校", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CtSchool ctSchool)
    {
        List<CtSchool> list = ctSchoolService.selectCtSchoolList(ctSchool);
        ExcelUtil<CtSchool> util = new ExcelUtil<CtSchool>(CtSchool.class);
        util.exportExcel(response, list, "学校数据");
    }

    @PreAuthorize("@ss.hasPermi('campus:school:query')")
    @GetMapping(value = "/{schoolId}")
    public AjaxResult getInfo(@PathVariable("schoolId") Long schoolId)
    {
        return success(ctSchoolService.selectCtSchoolById(schoolId));
    }

    @PreAuthorize("@ss.hasPermi('campus:school:add')")
    @Log(title = "学校", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CtSchool ctSchool)
    {
        ctSchool.setCreateBy(getUsername());
        return toAjax(ctSchoolService.insertCtSchool(ctSchool));
    }

    @PreAuthorize("@ss.hasPermi('campus:school:edit')")
    @Log(title = "学校", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CtSchool ctSchool)
    {
        ctSchool.setUpdateBy(getUsername());
        return toAjax(ctSchoolService.updateCtSchool(ctSchool));
    }

    @PreAuthorize("@ss.hasPermi('campus:school:remove')")
    @Log(title = "学校", businessType = BusinessType.DELETE)
    @DeleteMapping("/{schoolIds}")
    public AjaxResult remove(@PathVariable Long[] schoolIds)
    {
        return toAjax(ctSchoolService.deleteCtSchoolByIds(schoolIds));
    }
}
