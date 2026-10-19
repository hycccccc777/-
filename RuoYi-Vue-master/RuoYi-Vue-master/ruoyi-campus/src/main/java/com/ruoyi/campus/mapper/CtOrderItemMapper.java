package com.ruoyi.campus.mapper;

import java.util.List;
import com.ruoyi.campus.domain.CtOrderItem;

/**
 * 订单明细 数据层
 * 
 * @author campus
 */
public interface CtOrderItemMapper
{
    public List<CtOrderItem> selectCtOrderItemList(CtOrderItem ctOrderItem);

    public CtOrderItem selectCtOrderItemById(Long itemId);

    /**
     * 根据订单ID查询订单明细
     *
     * @param orderId 订单ID
     * @return 订单明细列表
     */
    public List<CtOrderItem> selectCtOrderItemByOrderId(Long orderId);

    public int insertCtOrderItem(CtOrderItem ctOrderItem);

    public int updateCtOrderItem(CtOrderItem ctOrderItem);

    public int deleteCtOrderItemById(Long itemId);

    public int deleteCtOrderItemByIds(Long[] itemIds);

    public int deleteCtOrderItemByOrderId(Long orderId);
}
