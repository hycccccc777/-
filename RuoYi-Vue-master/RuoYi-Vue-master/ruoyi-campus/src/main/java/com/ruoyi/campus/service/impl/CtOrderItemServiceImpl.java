package com.ruoyi.campus.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.campus.domain.CtOrderItem;
import com.ruoyi.campus.mapper.CtOrderItemMapper;
import com.ruoyi.campus.service.ICtOrderItemService;

/**
 * 订单明细 服务层实现
 * 
 * @author campus
 */
@Service
public class CtOrderItemServiceImpl implements ICtOrderItemService
{
    @Autowired
    private CtOrderItemMapper ctOrderItemMapper;

    @Override
    public List<CtOrderItem> selectCtOrderItemList(CtOrderItem ctOrderItem)
    {
        return ctOrderItemMapper.selectCtOrderItemList(ctOrderItem);
    }

    @Override
    public CtOrderItem selectCtOrderItemById(Long itemId)
    {
        return ctOrderItemMapper.selectCtOrderItemById(itemId);
    }

    @Override
    public List<CtOrderItem> selectCtOrderItemByOrderId(Long orderId)
    {
        return ctOrderItemMapper.selectCtOrderItemByOrderId(orderId);
    }

    @Override
    public int insertCtOrderItem(CtOrderItem ctOrderItem)
    {
        return ctOrderItemMapper.insertCtOrderItem(ctOrderItem);
    }

    @Override
    public int updateCtOrderItem(CtOrderItem ctOrderItem)
    {
        return ctOrderItemMapper.updateCtOrderItem(ctOrderItem);
    }

    @Override
    public int deleteCtOrderItemByIds(Long[] itemIds)
    {
        return ctOrderItemMapper.deleteCtOrderItemByIds(itemIds);
    }

    @Override
    public int deleteCtOrderItemByOrderId(Long orderId)
    {
        return ctOrderItemMapper.deleteCtOrderItemByOrderId(orderId);
    }
}
