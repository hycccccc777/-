package com.ruoyi.campus.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.campus.domain.CtPayment;

/**
 * 支付单 服务层
 *
 * @author campus
 */
public interface ICtPaymentService
{
    public List<CtPayment> selectCtPaymentList(CtPayment ctPayment);

    public CtPayment selectCtPaymentById(Long paymentId);

    public int insertCtPayment(CtPayment ctPayment);

    public int updateCtPayment(CtPayment ctPayment);

    public int deleteCtPaymentByIds(Long[] paymentIds);

    /**
     * 创建支付单：按 payMethod 选择对应网关完成预下单。
     */
    public Map<String, Object> create(Long userId, String orderNo, String payMethod);

    /**
     * 查询支付状态
     */
    public Map<String, Object> getStatus(Long userId, String payNo);

    /**
     * 主动确认支付（仅支持该方式的网关可用，如沙箱/模拟网关），
     * 并把订单推进为「待发货」。重复调用幂等。
     */
    public Map<String, Object> confirm(Long userId, String payNo);

    /**
     * 渠道异步回调：校验签名后把支付单置为成功并推进订单。
     * 真实支付网关走的是这条路径。
     */
    public Map<String, Object> handleCallback(String payNo, String sign);
}