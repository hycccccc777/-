package com.ruoyi.campus.service;

import java.util.List;
import com.ruoyi.campus.domain.CtOrderItem;

/**
 * 订单明细 服务层
 * 
 * @author campus
 */
public interface ICtOrderItemService
{
    public List<CtOrderItem> selectCtOrderItemList(CtOrderItem ctOrderItem);

    public CtOrderItem selectCtOrderItemById(Long itemId);

    public List<CtOrderItem> selectCtOrderItemByOrderId(Long orderId);

    public int insertCtOrderItem(CtOrderItem ctOrderItem);

    public int updateCtOrderItem(CtOrderItem ctOrderItem);

    public int deleteCtOrderItemByIds(Long[] itemIds);

    public int deleteCtOrderItemByOrderId(Long orderId);
}
