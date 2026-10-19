package com.ruoyi.campus.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 面向 /api/v1 前端的图片上传接口（需登录）
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "文件上传", description = "图片上传")
public class ApiUploadController
{
    /**
     * 上传图片，返回可访问的相对地址（/profile/upload/...）
     */
    @PostMapping("/upload")
    @Operation(summary = "上传图片", description = "返回可访问相对地址，需登录")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            return ApiResp.fail(400, "请选择要上传的图片");
        }
        try
        {
            String url = FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file, MimeTypeUtils.IMAGE_EXTENSION);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("url", url);
            data.put("fileName", url);
            return ApiResp.ok(data);
        }
        catch (Exception e)
        {
            return ApiResp.fail(400, "图片上传失败：" + e.getMessage());
        }
    }
}
