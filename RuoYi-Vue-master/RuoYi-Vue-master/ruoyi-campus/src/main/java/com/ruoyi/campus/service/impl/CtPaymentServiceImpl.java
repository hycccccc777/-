package com.ruoyi.campus.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.domain.CtOrder;
import com.ruoyi.campus.domain.CtPayment;
import com.ruoyi.campus.mapper.CtOrderMapper;
import com.ruoyi.campus.mapper.CtPaymentMapper;
import com.ruoyi.campus.payment.PaymentGateway;
import com.ruoyi.campus.payment.PaymentGatewayRegistry;
import com.ruoyi.campus.service.ICtPaymentService;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;

/**
 * 支付单 服务层实现
 *
 * <p>具体支付渠道由 {@link PaymentGatewayRegistry} 分发，本类只负责
 * 「支付单与订单的状态一致性」，不关心钱是怎么收的。
 *
 * @author campus
 */
@Service
public class CtPaymentServiceImpl implements ICtPaymentService
{
    private static final Map<String, String> PAY_STATUS = new HashMap<>();

    static
    {
        PAY_STATUS.put("0", "待支付");
        PAY_STATUS.put("1", "支付成功");
        PAY_STATUS.put("2", "支付失败");
        PAY_STATUS.put("3", "已退款");
    }

    @Autowired
    private CtPaymentMapper ctPaymentMapper;

    @Autowired
    private CtOrderMapper ctOrderMapper;

    @Autowired
    private PaymentGatewayRegistry gatewayRegistry;

    /**
     * 默认支付方式：前端不传 payMethod 时使用。
     * 开发默认沙箱(0)，生产 profile 默认线下面交(4)。
     */
    @Value("${campus.payment.method:0}")
    private String defaultPayMethod;

    @Override
    public List<CtPayment> selectCtPaymentList(CtPayment ctPayment)
    {
        return ctPaymentMapper.selectCtPaymentList(ctPayment);
    }

    @Override
    public CtPayment selectCtPaymentById(Long paymentId)
    {
        return ctPaymentMapper.selectCtPaymentById(paymentId);
    }

    @Override
    public int insertCtPayment(CtPayment ctPayment)
    {
        return ctPaymentMapper.insertCtPayment(ctPayment);
    }

    @Override
    public int updateCtPayment(CtPayment ctPayment)
    {
        return ctPaymentMapper.updateCtPayment(ctPayment);
    }

    @Override
    public int deleteCtPaymentByIds(Long[] paymentIds)
    {
        for (Long paymentId : paymentIds)
        {
            ctPaymentMapper.deleteCtPaymentById(paymentId);
        }
        return paymentIds.length;
    }

    @Override
    public Map<String, Object> create(Long userId, String orderNo, String payMethod)
    {
        if (StringUtils.isEmpty(orderNo))
        {
            throw new ApiException(400, "缺少订单号");
        }
        CtOrder order = ctOrderMapper.selectCtOrderByOrderNo(orderNo);
        if (order == null || !userId.equals(order.getBuyerId()))
        {
            throw new ApiException(404, "订单不存在");
        }
        if (!"0".equals(order.getStatus()))
        {
            // 已进入支付之后的环节：直接返回已有支付单，避免重复创建
            List<CtPayment> existing = findByOrderNo(orderNo);
            if (!existing.isEmpty())
            {
                return toMap(existing.get(0));
            }
            throw new ApiException(409, "订单当前状态不可支付");
        }

        String method = StringUtils.isEmpty(payMethod) ? defaultPayMethod : payMethod;
        PaymentGateway gateway = gatewayRegistry.resolve(method);

        CtPayment payment = new CtPayment();
        payment.setPayNo(generatePayNo());
        payment.setOrderNo(orderNo);
        payment.setUserId(userId);
        payment.setAmount(order.getPayAmount());
        payment.setPayMethod(gateway.code());
        payment.setStatus("0");
        payment.setThirdPartyNo("");
        ctPaymentMapper.insertCtPayment(payment);

        // 预下单凭据：真实网关这里是支付串/跳转链接，前端拿它唤起收银台
        String prepayData = gateway.prepay(payment.getPayNo(), payment.getAmount());
        Map<String, Object> result = toMap(payment);
        result.put("gateway", gateway.name());
        result.put("prepayData", prepayData);
        return result;
    }

    @Override
    public Map<String, Object> getStatus(Long userId, String payNo)
    {
        return toMap(requireOwnPayment(userId, payNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirm(Long userId, String payNo)
    {
        CtPayment payment = requireOwnPayment(userId, payNo);
        PaymentGateway gateway = gatewayRegistry.require(payment.getPayMethod());
        if (!gateway.supportsManualConfirm())
        {
            throw new ApiException(400, "「" + gateway.name() + "」不支持主动确认，请等待渠道支付结果");
        }
        return markPaid(payment, gateway.confirm(payment.getPayNo(), payment.getAmount()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleCallback(String payNo, String sign)
    {
        CtPayment probe = new CtPayment();
        probe.setPayNo(payNo);
        List<CtPayment> list = ctPaymentMapper.selectCtPaymentList(probe);
        if (list.isEmpty())
        {
            throw new ApiException(404, "支付单不存在");
        }
        CtPayment payment = list.get(0);
        PaymentGateway gateway = gatewayRegistry.require(payment.getPayMethod());
        if (!gateway.verifyCallback(payNo, sign))
        {
            // 签名不对一律拒绝：否则任何人都能伪造支付成功
            throw new ApiException(403, "回调签名校验失败");
        }
        return markPaid(payment, "CALLBACK" + IdUtils.fastSimpleUUID().substring(0, 12).toUpperCase());
    }

    /**
     * 支付单置为成功并推进订单。已成功则直接返回，保证回调与主动确认都幂等。
     */
    private Map<String, Object> markPaid(CtPayment payment, String tradeNo)
    {
        if ("1".equals(payment.getStatus()))
        {
            return toMap(payment);
        }
        if (!"0".equals(payment.getStatus()))
        {
            throw new ApiException(409, "支付单当前状态不可确认");
        }
        payment.setStatus("1");
        payment.setThirdPartyNo(tradeNo);
        payment.setCallbackTime(new Date());
        ctPaymentMapper.updateCtPayment(payment);

        CtOrder order = ctOrderMapper.selectCtOrderByOrderNo(payment.getOrderNo());
        if (order == null)
        {
            throw new ApiException(404, "订单不存在");
        }
        if (!"0".equals(order.getStatus()))
        {
            throw new ApiException(409, "订单当前状态不可支付");
        }
        order.setStatus("1");
        order.setPayTime(new Date());
        ctOrderMapper.updateCtOrder(order);
        return toMap(payment);
    }

    private CtPayment requireOwnPayment(Long userId, String payNo)
    {
        CtPayment probe = new CtPayment();
        probe.setPayNo(payNo);
        List<CtPayment> list = ctPaymentMapper.selectCtPaymentList(probe);
        if (list.isEmpty() || !userId.equals(list.get(0).getUserId()))
        {
            throw new ApiException(404, "支付单不存在");
        }
        return list.get(0);
    }

    private List<CtPayment> findByOrderNo(String orderNo)
    {
        CtPayment probe = new CtPayment();
        probe.setOrderNo(orderNo);
        return ctPaymentMapper.selectCtPaymentList(probe);
    }

    private Map<String, Object> toMap(CtPayment p)
    {
        PaymentGateway gateway = gatewayRegistry.resolve(p.getPayMethod());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("payNo", p.getPayNo());
        m.put("orderNo", p.getOrderNo());
        m.put("amount", p.getAmount());
        m.put("payMethod", p.getPayMethod());
        m.put("payMethodLabel", gateway.name());
        m.put("status", p.getStatus());
        m.put("statusText", PAY_STATUS.get(p.getStatus()));
        m.put("thirdPartyNo", p.getThirdPartyNo());
        m.put("createTime", p.getCreateTime());
        m.put("callbackTime", p.getCallbackTime());
        return m;
    }

    private String generatePayNo()
    {
        return "PAY" + DateUtils.dateTimeNow("yyyyMMddHHmmss") + IdUtils.fastSimpleUUID().substring(0, 6).toUpperCase();
    }
}