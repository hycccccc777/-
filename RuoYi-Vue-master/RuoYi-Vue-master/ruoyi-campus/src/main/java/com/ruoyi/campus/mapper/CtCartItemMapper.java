package com.ruoyi.campus.mapper;

import java.util.List;
import com.ruoyi.campus.domain.CtCartItem;

/**
 * 购物车条目 数据层
 * 
 * @author campus
 */
public interface CtCartItemMapper
{
    public List<CtCartItem> selectCtCartItemList(CtCartItem ctCartItem);

    public CtCartItem selectCtCartItemById(Long cartItemId);

    public int insertCtCartItem(CtCartItem ctCartItem);

    public int updateCtCartItem(CtCartItem ctCartItem);

    public int deleteCtCartItemById(Long cartItemId);

    public int deleteCtCartItemByIds(Long[] cartItemIds);
}
