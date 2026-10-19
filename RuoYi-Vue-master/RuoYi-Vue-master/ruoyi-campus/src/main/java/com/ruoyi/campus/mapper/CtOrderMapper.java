package com.ruoyi.campus.mapper;

import java.util.List;
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
}
