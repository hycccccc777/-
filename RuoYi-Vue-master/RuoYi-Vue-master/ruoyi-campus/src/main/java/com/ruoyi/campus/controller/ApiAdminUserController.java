package com.ruoyi.campus.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 用户管理（管理员只读）。
 *
 * <p>管理端前端的最小可用视图：让管理员能看见平台用户列表与状态。
 * 写操作（停用/启用/重置密码）仍走若依自带的 /system/user，这里只读。
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "用户管理", description = "管理员查看用户列表")
public class ApiAdminUserController
{
    @Autowired
    private ISysUserService userService;

    @PreAuthorize("@ss.hasRole('admin')")
    @GetMapping("/users")
    @Operation(summary = "用户列表", description = "仅管理员；按用户名/手机号模糊过滤")
    public ResponseEntity<Map<String, Object>> users(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size)
    {
        if (page < 1)
        {
            page = 1;
        }
        if (size < 1 || size > 100)
        {
            size = 20;
        }
        SysUser query = new SysUser();
        if (keyword != null && !keyword.trim().isEmpty())
        {
            query.setUserName(keyword.trim());
            query.setPhonenumber(keyword.trim());
        }
        PageHelper.startPage(page, size);
        try
        {
            List<SysUser> list = userService.selectUserList(query);
            PageInfo<SysUser> info = new PageInfo<>(list);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (SysUser u : list)
            {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", String.valueOf(u.getUserId()));
                m.put("username", u.getUserName());
                m.put("nickname", u.getNickName());
                m.put("phone", u.getPhonenumber());
                m.put("status", u.getStatus());
                m.put("createTime", u.getCreateTime());
                rows.add(m);
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("total", info.getTotal());
            data.put("page", page);
            data.put("size", size);
            data.put("list", rows);
            return ApiResp.ok(data);
        }
        finally
        {
            PageHelper.clearPage();
        }
    }
}
