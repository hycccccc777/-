package com.ruoyi.campus.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 支付回调签名校验。
 *
 * <p>支付回调接口对外暴露且无需登录，如果不校验签名，任何人都能伪造
 * 「支付成功」把订单刷成已付款。因此这里用共享密钥做签名比对。
 *
 * <p>密钥从配置读取（环境变量 CAMPUS_PAY_SECRET 优先），不在代码里写死。
 *
 * @author campus
 */
@Component
public class PaymentCallbackSecret
{
    private final String secret;

    public PaymentCallbackSecret(@Value("${campus.payment.notify-secret:change-me-in-production}") String secret)
    {
        this.secret = secret;
    }

    /**
     * 计算期望签名。
     */
    public String sign(String payNo)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((secret + ":" + payNo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("计算回调签名失败", e);
        }
    }

    public boolean isValid(String payNo, String sign)
    {
        return sign != null && sign.equalsIgnoreCase(this.sign(payNo));
    }
}