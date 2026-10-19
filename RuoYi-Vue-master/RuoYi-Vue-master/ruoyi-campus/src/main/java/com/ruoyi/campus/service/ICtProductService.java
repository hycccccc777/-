package com.ruoyi.campus.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.campus.domain.CtProduct;
import com.ruoyi.campus.domain.CtProductCreateBody;
import com.ruoyi.campus.domain.vo.CtProductVo;

/**
 * ??? ?????
 * 
 * @author campus
 */
public interface ICtProductService
{
    public List<CtProduct> selectCtProductList(CtProduct ctProduct);

    public CtProduct selectCtProductById(Long productId);

    public List<CtProductVo> selectCtProductVoList(CtProductVo ctProductVo);

    public CtProductVo selectCtProductVoById(Long productId);

    /**
     * 发布商品（含图片）
     */
    public Map<String, Object> publishProduct(CtProductCreateBody body);

    /**
     * 原子扣减库存（返回 true 表示扣减成功）
     */
    public boolean deductStock(Long productId, int qty);

    /**
     * 原子回滚库存
     */
    public void restoreStock(Long productId, int qty);

    /**
     * 我发布的商品（status 可为空表示全部）
     */
    public List<CtProductVo> selectMyProducts(Long userId, String status);

    /**
     * 卖家编辑自己的商品
     */
    public int updateMyProduct(Long userId, Long productId, CtProduct patch);

    /**
     * 上架/下架（status 只允许 1 或 2）
     */
    public int updateMyProductStatus(Long userId, Long productId, String status);

    /**
     * 删除自己的商品（软删除）
     */
    public int removeMyProduct(Long userId, Long productId);

    public int insertCtProduct(CtProduct ctProduct);

    public int updateCtProduct(CtProduct ctProduct);

    public int deleteCtProductByIds(Long[] productIds);
}
