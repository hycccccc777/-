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
import com.ruoyi.campus.domain.CtUserProfile;
import com.ruoyi.campus.service.ICtUserProfileService;

/**
 * 用户校园档案Controller
 * 
 * @author campus
 */
@RestController
@RequestMapping("/campus/profile")
public class CtUserProfileController extends BaseController
{
    @Autowired
    private ICtUserProfileService ctUserProfileService;

    @PreAuthorize("@ss.hasPermi('campus:profile:list')")
    @GetMapping("/list")
    public TableDataInfo list(CtUserProfile ctUserProfile)
    {
        startPage();
        List<CtUserProfile> list = ctUserProfileService.selectCtUserProfileList(ctUserProfile);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('campus:profile:export')")
    @Log(title = "用户校园档案", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CtUserProfile ctUserProfile)
    {
        List<CtUserProfile> list = ctUserProfileService.selectCtUserProfileList(ctUserProfile);
        ExcelUtil<CtUserProfile> util = new ExcelUtil<CtUserProfile>(CtUserProfile.class);
        util.exportExcel(response, list, "用户校园档案数据");
    }

    @PreAuthorize("@ss.hasPermi('campus:profile:query')")
    @GetMapping(value = "/{profileId}")
    public AjaxResult getInfo(@PathVariable("profileId") Long profileId)
    {
        return success(ctUserProfileService.selectCtUserProfileById(profileId));
    }

    @PreAuthorize("@ss.hasPermi('campus:profile:add')")
    @Log(title = "用户校园档案", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CtUserProfile ctUserProfile)
    {
        ctUserProfile.setCreateBy(getUsername());
        return toAjax(ctUserProfileService.insertCtUserProfile(ctUserProfile));
    }

    @PreAuthorize("@ss.hasPermi('campus:profile:edit')")
    @Log(title = "用户校园档案", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody CtUserProfile ctUserProfile)
    {
        ctUserProfile.setUpdateBy(getUsername());
        return toAjax(ctUserProfileService.updateCtUserProfile(ctUserProfile));
    }

    @PreAuthorize("@ss.hasPermi('campus:profile:remove')")
    @Log(title = "用户校园档案", businessType = BusinessType.DELETE)
    @DeleteMapping("/{profileIds}")
    public AjaxResult remove(@PathVariable Long[] profileIds)
    {
        return toAjax(ctUserProfileService.deleteCtUserProfileByIds(profileIds));
    }
}
