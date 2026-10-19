package com.ruoyi.campus.mapper;

import java.util.List;
import com.ruoyi.campus.domain.CtRefund;

/**
 * 退款/售后 数据层
 * 
 * @author campus
 */
public interface CtRefundMapper
{
    public List<CtRefund> selectCtRefundList(CtRefund ctRefund);

    public CtRefund selectCtRefundById(Long refundId);

    public int insertCtRefund(CtRefund ctRefund);

    public int updateCtRefund(CtRefund ctRefund);

    public int deleteCtRefundById(Long refundId);

    public int deleteCtRefundByIds(Long[] refundIds);
}
