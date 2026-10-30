package com.ruoyi.campus.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.campus.api.ApiResp;
import com.ruoyi.campus.storage.FileStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 面向 /api/v1 前端的图片上传接口（需登录）。
 *
 * <p>具体落盘位置由 {@link FileStorage} 决定：本地磁盘或腾讯云 COS，
 * 换存储不影响接口。
 *
 * @author campus
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "文件上传", description = "图片上传")
public class ApiUploadController
{
    @Autowired
    private FileStorage fileStorage;

    /** 单张图片大小上限（字节），默认 10MB */
    @Value("${campus.upload.max-bytes:10485760}")
    private long maxBytes;

    @PostMapping("/upload")
    @Operation(summary = "上传图片", description = "返回可访问地址，需登录")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            return ApiResp.fail(400, "请选择要上传的图片");
        }
        if (file.getSize() > maxBytes)
        {
            return ApiResp.fail(400, "图片过大，最大 " + (maxBytes / 1024 / 1024) + "MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/"))
        {
            return ApiResp.fail(400, "只允许上传图片");
        }
        try
        {
            String url = fileStorage.store(file);
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
