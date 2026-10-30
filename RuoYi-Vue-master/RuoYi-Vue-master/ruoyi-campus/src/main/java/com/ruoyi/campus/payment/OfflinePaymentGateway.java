package com.ruoyi.campus.payment;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * 线下面交/到付网关。
 *
 * <p>校园二手大量交易是当面验货、当面付款，平台不接触资金。该网关
 * 不连任何支付渠道：买家下单后，见面付款完成时由买家在订单里点“确认
 * 已付款”，即可把订单推进到待交付。
 *
 * <p>它让系统在没有支付商户号的情况下也能完整跑通交易闭环，同时
 * 不留下“沙箱直接成功”这种上线隐患——生产 profile 默认就用这个网关。
 *
 * @author campus
 */
@Component
public class OfflinePaymentGateway implements PaymentGateway
{
    /** 支付方式编码：4=线下面交 */
    public static final String CODE = "4";

    @Override
    public String code()
    {
        return CODE;
    }

    @Override
    public String name()
    {
        return "线下面交";
    }

    @Override
    public String prepay(String payNo, BigDecimal amount)
    {
        return "OFFLINE:" + payNo;
    }

    @Override
    public String confirm(String payNo, BigDecimal amount)
    {
        return "OFFLINE" + IdUtils.fastSimpleUUID().substring(0, 12).toUpperCase();
    }

    @Override
    public boolean supportsManualConfirm()
    {
        return true;
    }

    @Override
    public boolean verifyCallback(String payNo, String sign)
    {
        // 线下面交没有渠道回调，任何回调都视为非法
        return false;
    }
}
