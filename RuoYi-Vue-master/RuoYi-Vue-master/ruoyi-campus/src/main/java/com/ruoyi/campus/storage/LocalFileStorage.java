package com.ruoyi.campus.storage;

import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;

/**
 * 本地磁盘存储：写到 ruoyi.profile 指定目录，返回 /profile/... 相对地址，
 * 由 Nginx 或后端静态资源映射对外提供。
 *
 * @author campus
 */
public class LocalFileStorage implements FileStorage
{
    @Override
    public String store(MultipartFile file)
    {
        try
        {
            return FileUploadUtils.upload(RuoYiConfig.getUploadPath(), file, MimeTypeUtils.IMAGE_EXTENSION);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("本地存储失败：" + e.getMessage(), e);
        }
    }

    @Override
    public String type()
    {
        return "local";
    }
}
