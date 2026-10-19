package com.ruoyi.campus.payment;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * 支付结果模拟网关（仅用于演示与测试异常分支）。
 *
 * <p>它存在的意义不是「再做一个支付渠道」，而是让**失败、超时**这些在
 * 沙箱网关里永远走不到的分支也能被测到：
 *
 * <ul>
 *   <li>支付金额 ≥ 500 元 → 确认时返回失败</li>
 *   <li>其余金额 → 正常成功</li>
 * </ul>
 *
 * @author campus
 */
@Component
public class MockPaymentGateway implements PaymentGateway
{
    public static final String CODE = "9";

    /** 金额达到该值即模拟失败，便于测试异常分支 */
    public static final double MOCK_FAIL_THRESHOLD = 500d;

    @Override
    public String code()
    {
        return CODE;
    }

    @Override
    public String name()
    {
        return "支付模拟网关";
    }

    @Override
    public String prepay(String payNo, BigDecimal amount)
    {
        return "MOCK_PREPAY:" + payNo + ":" + amount;
    }

    @Override
    public String confirm(String payNo, BigDecimal amount)
    {
        // 用金额触发失败分支：payNo 是服务端随机生成的，不可靠；
        // 金额由用例控制，才能保证这个「沙箱里永远走不到」的路径也被覆盖。
        if (amount != null && amount.doubleValue() >= MOCK_FAIL_THRESHOLD)
        {
            throw new ApiException(409, "模拟支付失败：渠道返回「交易被拒绝」");
        }
        return "MOCK" + IdUtils.fastSimpleUUID().substring(0, 12).toUpperCase();
    }

    @Override
    public boolean supportsManualConfirm()
    {
        return true;
    }

    @Override
    public boolean verifyCallback(String payNo, String sign)
    {
        // 模拟网关只用于内网演示，同样要求签名，避免留下「无鉴权回调」的坏示范
        return false;
    }
}