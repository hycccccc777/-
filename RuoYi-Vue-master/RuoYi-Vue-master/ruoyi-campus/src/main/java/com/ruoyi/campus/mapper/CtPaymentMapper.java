package com.ruoyi.campus.mapper;

import java.util.List;
import com.ruoyi.campus.domain.CtPayment;

/**
 * 支付单 数据层
 * 
 * @author campus
 */
public interface CtPaymentMapper
{
    public List<CtPayment> selectCtPaymentList(CtPayment ctPayment);

    public CtPayment selectCtPaymentById(Long paymentId);

    public CtPayment selectCtPaymentByPayNo(String payNo);

    public int insertCtPayment(CtPayment ctPayment);

    public int updateCtPayment(CtPayment ctPayment);

    public int deleteCtPaymentById(Long paymentId);

    public int deleteCtPaymentByIds(Long[] paymentIds);
}
