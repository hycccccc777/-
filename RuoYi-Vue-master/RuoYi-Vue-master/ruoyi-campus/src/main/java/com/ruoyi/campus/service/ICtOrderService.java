package com.ruoyi.campus.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.campus.domain.CtOrder;
import com.ruoyi.campus.domain.CtOrderCreateBody;

/**
 * 订单 服务层
 * 
 * @author campus
 */
public interface ICtOrderService
{
    public List<CtOrder> selectCtOrderList(CtOrder ctOrder);

    public CtOrder selectCtOrderById(Long orderId);

    public int insertCtOrder(CtOrder ctOrder);

    public int updateCtOrder(CtOrder ctOrder);

    public int deleteCtOrderByIds(Long[] orderIds);

    /**
     * 下单：按卖家拆单，同一幂等键重复提交返回同一批订单
     */
    public Map<String, Object> createOrder(CtOrderCreateBody body);

    /**
     * 我买到的订单
     */
    public List<Map<String, Object>> listMyOrders(Long userId);

    /**
     * 我卖出的订单
     */
    public List<Map<String, Object>> listMySales(Long userId);

    /**
     * 订单详情（买家或卖家可见）
     */
    public Map<String, Object> orderDetail(Long userId, String orderNo);

    /**
     * 取消订单（仅待付款可取消，取消后恢复库存）
     */
    public void cancelOrder(Long userId, String orderNo, String reason);

    /**
     * 确认收货（仅待收货可确认）
     */
    public void confirmOrder(Long userId, String orderNo);

    /**
     * 卖家发货（仅待发货可发货）
     */
    public void shipOrder(Long userId, String orderNo);

    /**
     * 取消超过指定分钟数仍未支付的订单，并恢复库存。
     *
     * <p>由定时任务调用。逐单条件更新，多实例并发执行也只会成功取消一次。
     *
     * @param timeoutMinutes 超时分钟数，小于等于 0 表示不启用
     * @param batchLimit     单次最多处理条数
     * @return 本次实际取消的订单数
     */
    public int cancelTimeoutOrders(int timeoutMinutes, int batchLimit);
}