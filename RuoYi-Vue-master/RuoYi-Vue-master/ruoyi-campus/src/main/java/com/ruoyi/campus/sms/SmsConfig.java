package com.ruoyi.campus.sms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 短信发送器装配。
 *
 * <p>provider=tencent 且配置了 secretId 时用腾讯云；否则用日志发送器（本地/测试）。
 * 这样没有短信凭据也能启动，不会因为缺配置直接起不来。
 *
 * @author campus
 */
@Configuration
public class SmsConfig
{
    @Bean
    public SmsSender smsSender(
            @Value("${campus.sms.provider:log}") String provider,
            @Value("${campus.sms.tencent.secret-id:}") String secretId,
            @Value("${campus.sms.tencent.secret-key:}") String secretKey,
            @Value("${campus.sms.tencent.sdk-app-id:}") String sdkAppId,
            @Value("${campus.sms.tencent.sign-name:}") String signName,
            @Value("${campus.sms.tencent.template-id:}") String templateId,
            @Value("${campus.sms.tencent.region:ap-guangzhou}") String region)
    {
        if ("tencent".equalsIgnoreCase(provider) && secretId != null && !secretId.isBlank())
        {
            return new TencentSmsSender(secretId, secretKey, sdkAppId, signName, templateId, region);
        }
        return new LogSmsSender();
    }
}
