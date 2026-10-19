package com.ruoyi.campus.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.campus.domain.CtCartItem;

/**
 * 购物车条目 服务层
 * 
 * @author campus
 */
public interface ICtCartItemService
{
    public List<CtCartItem> selectCtCartItemList(CtCartItem ctCartItem);

    public CtCartItem selectCtCartItemById(Long cartItemId);

    public int insertCtCartItem(CtCartItem ctCartItem);

    public int updateCtCartItem(CtCartItem ctCartItem);

    public int deleteCtCartItemByIds(Long[] cartItemIds);

    /**
     * 我的购物车（含商品摘要）
     */
    public List<Map<String, Object>> listWithProduct(Long userId);

    /**
     * 加入购物车（已存在则累加数量）
     */
    public Map<String, Object> addItem(Long userId, Long productId, Integer quantity);

    /**
     * 修改数量
     */
    public Map<String, Object> updateItem(Long userId, Long cartItemId, Integer quantity);

    /**
     * 移除条目
     */
    public void removeItem(Long userId, Long cartItemId);
}
