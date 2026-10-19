package com.ruoyi.campus.payment;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * 沙箱支付网关。
 *
 * <p>用于演示与自动化测试：不连任何真实渠道，确认即成功。
 * 之所以保留成独立实现而不是写死在 Service 里，就是为了验证
 * 「换支付渠道不改业务代码」这件事真的成立。
 *
 * @author campus
 */
@Component
public class SandboxPaymentGateway implements PaymentGateway
{
    private final PaymentCallbackSecret callbackSecret;

    public SandboxPaymentGateway(PaymentCallbackSecret callbackSecret)
    {
        this.callbackSecret = callbackSecret;
    }

    @Override
    public String code()
    {
        return "0";
    }

    @Override
    public String name()
    {
        return "沙箱支付";
    }

    @Override
    public String prepay(String payNo, BigDecimal amount)
    {
        return "SANDBOX_PREPAY:" + payNo + ":" + amount;
    }

    @Override
    public String confirm(String payNo, BigDecimal amount)
    {
        return "SANDBOX" + IdUtils.fastSimpleUUID().substring(0, 12).toUpperCase();
    }

    @Override
    public boolean supportsManualConfirm()
    {
        return true;
    }

    @Override
    public boolean verifyCallback(String payNo, String sign)
    {
        return callbackSecret.isValid(payNo, sign);
    }
}