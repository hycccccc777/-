package com.ruoyi.campus.domain;

/**
 * 购物车请求体（新增/修改数量共用）。
 *
 * @author campus
 */
public class CtCartBody
{
    /** 商品ID（新增时必填） */
    private Long productId;

    /** 购买数量 */
    private Integer quantity;

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }
}