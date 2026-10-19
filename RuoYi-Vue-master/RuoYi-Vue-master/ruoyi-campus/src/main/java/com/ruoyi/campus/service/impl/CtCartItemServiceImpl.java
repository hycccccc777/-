package com.ruoyi.campus.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.domain.CtCartItem;
import com.ruoyi.campus.domain.CtProduct;
import com.ruoyi.campus.domain.vo.CtProductVo;
import com.ruoyi.campus.mapper.CtCartItemMapper;
import com.ruoyi.campus.service.ICtCartItemService;
import com.ruoyi.campus.service.ICtProductService;

/**
 * 购物车条目 服务层实现
 * 
 * @author campus
 */
@Service
public class CtCartItemServiceImpl implements ICtCartItemService
{
    @Autowired
    private CtCartItemMapper ctCartItemMapper;

    @Autowired
    private ICtProductService productService;

    @Override
    public List<CtCartItem> selectCtCartItemList(CtCartItem ctCartItem)
    {
        return ctCartItemMapper.selectCtCartItemList(ctCartItem);
    }

    @Override
    public CtCartItem selectCtCartItemById(Long cartItemId)
    {
        return ctCartItemMapper.selectCtCartItemById(cartItemId);
    }

    @Override
    public int insertCtCartItem(CtCartItem ctCartItem)
    {
        return ctCartItemMapper.insertCtCartItem(ctCartItem);
    }

    @Override
    public int updateCtCartItem(CtCartItem ctCartItem)
    {
        return ctCartItemMapper.updateCtCartItem(ctCartItem);
    }

    @Override
    public int deleteCtCartItemByIds(Long[] cartItemIds)
    {
        return ctCartItemMapper.deleteCtCartItemByIds(cartItemIds);
    }

    @Override
    public List<Map<String, Object>> listWithProduct(Long userId)
    {
        CtCartItem query = new CtCartItem();
        query.setUserId(userId);
        List<CtCartItem> items = ctCartItemMapper.selectCtCartItemList(query);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CtCartItem item : items)
        {
            result.add(toCartMap(item, productService.selectCtProductVoById(item.getProductId())));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> addItem(Long userId, Long productId, Integer quantity)
    {
        if (productId == null)
        {
            throw new ApiException(400, "请选择要加入购物车的商品");
        }
        int qty = quantity == null || quantity < 1 ? 1 : quantity;
        CtProductVo product = productService.selectCtProductVoById(productId);
        if (product == null || !"1".equals(product.getStatus()))
        {
            throw new ApiException(409, "商品不存在或已下架");
        }
        if (product.getStock() != null && product.getStock() < qty)
        {
            throw new ApiException(409, "商品库存不足");
        }
        if (userId.equals(product.getSellerId()))
        {
            throw new ApiException(400, "不能购买自己发布的商品");
        }

        CtCartItem probe = new CtCartItem();
        probe.setUserId(userId);
        probe.setProductId(productId);
        List<CtCartItem> exists = ctCartItemMapper.selectCtCartItemList(probe);

        CtCartItem item;
        if (exists.isEmpty())
        {
            item = new CtCartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setQuantity(qty);
            item.setSelected("1");
            ctCartItemMapper.insertCtCartItem(item);
        }
        else
        {
            item = exists.get(0);
            item.setQuantity(item.getQuantity() + qty);
            ctCartItemMapper.updateCtCartItem(item);
        }
        return toCartMap(item, product);
    }

    @Override
    public Map<String, Object> updateItem(Long userId, Long cartItemId, Integer quantity)
    {
        CtCartItem item = requireOwnItem(userId, cartItemId);
        if (quantity == null || quantity < 1)
        {
            throw new ApiException(400, "购买数量至少为 1");
        }
        CtProductVo product = productService.selectCtProductVoById(item.getProductId());
        if (product != null && product.getStock() != null && product.getStock() < quantity)
        {
            throw new ApiException(409, "商品库存不足");
        }
        item.setQuantity(quantity);
        ctCartItemMapper.updateCtCartItem(item);
        return toCartMap(item, product);
    }

    @Override
    public void removeItem(Long userId, Long cartItemId)
    {
        CtCartItem item = requireOwnItem(userId, cartItemId);
        ctCartItemMapper.deleteCtCartItemById(item.getCartItemId());
    }

    private CtCartItem requireOwnItem(Long userId, Long cartItemId)
    {
        CtCartItem item = ctCartItemMapper.selectCtCartItemById(cartItemId);
        if (item == null || !userId.equals(item.getUserId()))
        {
            throw new ApiException(404, "购物车条目不存在");
        }
        return item;
    }

    private Map<String, Object> toCartMap(CtCartItem item, CtProductVo product)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(item.getCartItemId()));
        m.put("cartItemId", String.valueOf(item.getCartItemId()));
        m.put("productId", String.valueOf(item.getProductId()));
        m.put("quantity", item.getQuantity());
        m.put("selected", "1".equals(item.getSelected()));
        if (product != null)
        {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", String.valueOf(product.getProductId()));
            p.put("title", product.getTitle());
            p.put("price", product.getPrice());
            p.put("originalPrice", product.getOriginalPrice());
            p.put("coverUrl", product.getCoverUrl());
            p.put("conditionLabel", product.getConditionLabel());
            p.put("categoryId", product.getCategoryId());
            p.put("schoolName", product.getSchoolName());
            p.put("location", product.getLocation());
            p.put("sellerId", product.getSellerId());
            p.put("sellerName", product.getSellerName());
            p.put("status", product.getStatus());
            p.put("stock", product.getStock());
            m.put("product", p);
        }
        return m;
    }
}