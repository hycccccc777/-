package com.ruoyi.campus.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储装配。
 *
 * <p>type=cos 且配置了 secretId 时用腾讯云 COS，否则回退本地磁盘。
 * 没配对象存储也不会起不来，本地/测试直接可用。
 *
 * @author campus
 */
@Configuration
public class StorageConfig
{
    @Bean
    public FileStorage fileStorage(
            @Value("${campus.storage.type:local}") String type,
            @Value("${campus.storage.cos.secret-id:}") String secretId,
            @Value("${campus.storage.cos.secret-key:}") String secretKey,
            @Value("${campus.storage.cos.bucket:}") String bucket,
            @Value("${campus.storage.cos.region:ap-guangzhou}") String region,
            @Value("${campus.storage.cos.public-base-url:}") String publicBaseUrl,
            @Value("${campus.storage.cos.key-prefix:upload}") String keyPrefix)
    {
        if ("cos".equalsIgnoreCase(type) && secretId != null && !secretId.isBlank()
                && bucket != null && !bucket.isBlank())
        {
            return new TencentCosFileStorage(secretId, secretKey, bucket, region, publicBaseUrl, keyPrefix);
        }
        return new LocalFileStorage();
    }
}
