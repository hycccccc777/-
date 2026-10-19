package com.ruoyi.campus.service;

import java.util.List;
import java.util.Map;

/**
 * 退款/售后 服务层
 * 
 * @author campus
 */
public interface ICtRefundService
{
    /**
     * 买家申请退款（订单转「退款中」）
     */
    public Map<String, Object> apply(Long userId, String orderNo, String reason);

    /**
     * 卖家同意退款
     */
    public Map<String, Object> agree(Long sellerId, String refundNo);

    /**
     * 卖家拒绝退款（订单回滚到申请前状态）
     */
    public Map<String, Object> reject(Long sellerId, String refundNo, String remark);

    /**
     * 卖家确认退款到账（订单转「已退款」并回滚库存）
     */
    public Map<String, Object> complete(Long sellerId, String refundNo);

    /**
     * 我发起的退款
     */
    public List<Map<String, Object>> listMine(Long userId);

    /**
     * 我需要处理的退款（作为卖家）
     */
    public List<Map<String, Object>> listIncoming(Long sellerId);
}