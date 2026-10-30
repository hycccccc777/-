package com.ruoyi.campus.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象。上传位置（本地磁盘 / 对象存储）由配置决定，
 * 业务层只拿到一个可访问的 URL。
 *
 * @author campus
 */
public interface FileStorage
{
    /**
     * 保存一个文件，返回可对外访问的 URL（相对或绝对，取决于实现）。
     */
    String store(MultipartFile file);

    /** 存储类型标识：local / cos */
    String type();
}
