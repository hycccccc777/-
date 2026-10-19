package com.ruoyi.campus.payment;

import java.math.BigDecimal;

/**
 * 支付网关抽象。
 *
 * <p>业务层只依赖这个接口，不感知具体渠道。切换支付方式 = 换一个实现类，
 * 新增渠道不需要改动订单、支付单等业务代码。
 *
 * <p>与 sys_dict 的 pay_method 对应：0沙箱 1支付宝 2微信 3余额。
 *
 * @author campus
 */
public interface PaymentGateway
{
    /** 网关编码，与 pay_method 对齐 */
    String code();

    /** 网关名称 */
    String name();

    /**
     * 预下单：真实网关返回支付串或跳转链接，沙箱网关返回占位串。
     *
     * @param payNo  平台支付单号
     * @param amount 应付金额
     * @return 交给前端唤起支付的凭据
     */
    String prepay(String payNo, BigDecimal amount);

    /**
     * 主动确认支付结果。
     *
     * <p>只有沙箱/模拟网关会实现；真实网关的支付结果由渠道异步回调告知，
     * 调用方应改为走 {@link #verifyCallback} 校验签名后更新。
     *
     * @param amount 应付金额，部分渠道据此判定风控
     * @return 渠道流水号
     */
    String confirm(String payNo, BigDecimal amount);

    /**
     * 是否支持服务端主动确认。
     *
     * <p>真实网关返回 false，此时只能由 notify 回调推进订单。
     */
    boolean supportsManualConfirm();

    /**
     * 校验回调签名。
     *
     * <p>真实网关必须校验，否则任何人都能伪造「支付成功」把订单刷成已付款。
     * 沙箱网关返回固定值即可。
     *
     * @param sign 回调携带的签名
     * @return 签名是否合法
     */
    boolean verifyCallback(String payNo, String sign);
}