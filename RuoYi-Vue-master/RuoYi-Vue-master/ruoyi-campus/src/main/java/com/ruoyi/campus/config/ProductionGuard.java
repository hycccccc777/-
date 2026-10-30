package com.ruoyi.campus.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 生产环境启动自检。
 *
 * <p>带 {@code prod} profile 启动时，对“能直接造成资金/账
 * 号风险”的配置做硬校验：仍为开发默认值就直接拒绝启动，避免
 * 带着默认 JWT 密钥、通配 CORS、沙箱支付上线。
 *
 * <p>宁可启动失败，也不要静默地以不安全配置对外服务。
 *
 * @author campus
 */
@Component
@Profile("prod")
public class ProductionGuard
{
    private static final Logger log = LoggerFactory.getLogger(ProductionGuard.class);

    /** 代码里的 JWT 开发默认值，必须被替换 */
    private static final String DEFAULT_JWT_SECRET = "abcdefghijklmnopqrstuvwxyz";

    /** 支付回调密钥的占位值 */
    private static final String PLACEHOLDER_PAY_SECRET = "change-me-in-production";

    /** 线下面交支付方式编码 */
    private static final String OFFLINE_METHOD = "4";

    /** 沙箱支付方式编码 */
    private static final String SANDBOX_METHOD = "0";

    @Value("${token.secret:}")
    private String jwtSecret;

    @Value("${campus.payment.notify-secret:}")
    private String paySecret;

    @Value("${campus.payment.method:0}")
    private String payMethod;

    @Value("${campus.cors.allowed-origins:}")
    private String corsOrigins;

    @Value("${springdoc.api-docs.enabled:true}")
    private boolean apiDocsEnabled;

    @PostConstruct
    public void verify()
    {
        if (jwtSecret == null || jwtSecret.isBlank() || DEFAULT_JWT_SECRET.equals(jwtSecret)
                || jwtSecret.length() < 32)
        {
            fail("token.secret 仍为开发默认值或长度不足 32 位，请用 CAMPUS_JWT_SECRET 注入随机密钥");
        }

        if (SANDBOX_METHOD.equals(payMethod))
        {
            fail("生产环境不允许使用沙箱支付（campus.payment.method=0），请改为 4（线下面交）或真实网关");
        }

        if (!OFFLINE_METHOD.equals(payMethod)
                && (paySecret == null || paySecret.isBlank() || PLACEHOLDER_PAY_SECRET.equals(paySecret)))
        {
            fail("已启用在线支付但 campus.payment.notify-secret 为空/占位值，请用 CAMPUS_PAY_SECRET 注入");
        }

        if (corsOrigins != null && (corsOrigins.contains("*") || corsOrigins.contains("localhost")
                || corsOrigins.contains("127.0.0.1")))
        {
            fail("campus.cors.allowed-origins 仍含通配符或 localhost，请改为真实前端域名（CAMPUS_CORS_ORIGINS）");
        }

        if (apiDocsEnabled)
        {
            log.warn("接口文档在生产环境仍开启：建议 SPRINGDOC_ENABLED=false，避免暴露全部接口结构");
        }

        log.info("生产配置自检通过：支付方式={}，CORS={}", payMethod,
                (corsOrigins == null || corsOrigins.isBlank()) ? "同源" : corsOrigins);
    }

    private void fail(String message)
    {
        throw new IllegalStateException("[生产自检失败] " + message);
    }
}
