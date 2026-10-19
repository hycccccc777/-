package com.ruoyi.campus.payment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.ruoyi.campus.api.ApiException;

/**
 * 支付网关注册表：按 pay_method 找到对应实现。
 *
 * <p>所有网关实现只要是 Spring Bean 就会被收集进来，新增渠道零成本。
 * 未识别的支付方式一律降级到沙箱网关，避免演示环境直接不可用。
 *
 * @author campus
 */
@Component
public class PaymentGatewayRegistry
{
    private final Map<String, PaymentGateway> gateways = new LinkedHashMap<>();

    private final PaymentGateway fallback;

    public PaymentGatewayRegistry(List<PaymentGateway> discovered)
    {
        for (PaymentGateway gateway : discovered)
        {
            this.gateways.put(gateway.code(), gateway);
        }
        this.fallback = this.gateways.get(SandboxPaymentGatewayFallbackCode.SANDBOX);
    }

    /**
     * 按编码取网关，未知编码降级到沙箱。
     */
    public PaymentGateway resolve(String payMethod)
    {
        if (payMethod == null || payMethod.isEmpty())
        {
            return fallback;
        }
        PaymentGateway gateway = gateways.get(payMethod);
        return gateway == null ? fallback : gateway;
    }

    /**
     * 取网关，找不到直接报错（用于回调等必须精确匹配的场景）。
     */
    public PaymentGateway require(String payMethod)
    {
        PaymentGateway gateway = payMethod == null ? null : gateways.get(payMethod);
        if (gateway == null)
        {
            throw new ApiException(400, "不支持的支付方式：" + payMethod);
        }
        return gateway;
    }

    public List<String> supportedCodes()
    {
        return new java.util.ArrayList<>(gateways.keySet());
    }

    public Map<String, String> describe()
    {
        Map<String, String> map = new HashMap<>();
        for (PaymentGateway gateway : gateways.values())
        {
            map.put(gateway.code(), gateway.name());
        }
        return map;
    }

    /** 沙箱网关编码常量，避免各处硬编码字符串 */
    public static final class SandboxPaymentGatewayFallbackCode
    {
        public static final String SANDBOX = "0";

        private SandboxPaymentGatewayFallbackCode()
        {
        }
    }
}