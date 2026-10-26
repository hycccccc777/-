package com.ruoyi.campus.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.campus.domain.CtOrder;

/**
 * 订单 数据层
 * 
 * @author campus
 */
public interface CtOrderMapper
{
    public List<CtOrder> selectCtOrderList(CtOrder ctOrder);

    public CtOrder selectCtOrderById(Long orderId);

    public CtOrder selectCtOrderByOrderNo(String orderNo);

    public int insertCtOrder(CtOrder ctOrder);

    public int updateCtOrder(CtOrder ctOrder);

    public int deleteCtOrderById(Long orderId);

    public int deleteCtOrderByIds(Long[] orderIds);

    /**
     * 查询超过指定分钟数仍未支付的订单号。
     *
     * <p>时间比较放在 SQL 里用 NOW() 做，不在 Java 侧算 deadline：
     * 应用与数据库可能不在同一台机器、甚至时区不同，用应用时间去比较数据库里的
     * create_time 会出现"刚下单就被判超时"或"永远不超时"这类隐性偏差。
     *
     * @param minutes 超时分钟数
     * @param limit   单批最多处理条数，避免一次扫全表
     * @return 待取消的订单号
     */
    public List<String> selectTimeoutUnpaidOrderNos(@Param("minutes") int minutes, @Param("limit") int limit);

    /**
     * 仅当订单仍是「待付款」时才取消。
     *
     * <p>用条件更新而不是"先查后改"，是为了在多实例或多线程同时扫描时保证幂等：
     * 只有一个调用能拿到 affected=1，其余为 0，从而只回滚一次库存。
     *
     * @return 受影响行数（1=本次取消成功，0=已被别的线程/实例处理）
     */
    public int cancelUnpaidOrder(@Param("orderNo") String orderNo, @Param("cancelReason") String cancelReason);
}
