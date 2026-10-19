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
import com.ruoyi.campus.domain.CtFavorite;
import com.ruoyi.campus.service.ICtFavoriteService;

/**
 * 商品收藏Controller
 * 
 * @author campus
 */
@RestController
@RequestMapping("/campus/favorite")
public class CtFavoriteController extends BaseController
{
    @Autowired
    private ICtFavoriteService ctFavoriteService;

    @PreAuthorize("@ss.hasPermi('campus:favorite:list')")
    @GetMapping("/list")
    public TableDataInfo list(CtFavorite ctFavorite)
    {
        startPage();
        List<CtFavorite> list = ctFavoriteService.selectCtFavoriteList(ctFavorite);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('campus:favorite:export')")
    @Log(title = "商品收藏", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CtFavorite ctFavorite)
    {
        List<CtFavorite> list = ctFavoriteService.selectCtFavoriteList(ctFavorite);
        ExcelUtil<CtFavorite> util = new ExcelUtil<CtFavorite>(CtFavorite.class);
        util.exportExcel(response, list, "商品收藏数据");
    }

    @PreAuthorize("@ss.hasPermi('campus:favorite:query')")
    @GetMapping(value = "/{favoriteId}")
    public AjaxResult getInfo(@PathVariable("favoriteId") Long favoriteId)
    {
        return success(ctFavoriteService.selectCtFavoriteById(favoriteId));
    }

    @PreAuthorize("@ss.hasPermi('campus:favorite:add')")
    @Log(title = "商品收藏", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody CtFavorite ctFavorite)
    {
        return toAjax(ctFavoriteService.insertCtFavorite(ctFavorite));
    }

    @PreAuthorize("@ss.hasPermi('campus:favorite:remove')")
    @Log(title = "商品收藏", businessType = BusinessType.DELETE)
    @DeleteMapping("/{favoriteIds}")
    public AjaxResult remove(@PathVariable Long[] favoriteIds)
    {
        return toAjax(ctFavoriteService.deleteCtFavoriteByIds(favoriteIds));
    }
}
