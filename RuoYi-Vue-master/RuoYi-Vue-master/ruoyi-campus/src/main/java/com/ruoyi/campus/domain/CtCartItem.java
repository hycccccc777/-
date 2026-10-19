package com.ruoyi.campus.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 购物车条目对象 ct_cart_item
 * 
 * @author campus
 */
public class CtCartItem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 购物车条目ID（非商品ID） */
    private Long cartItemId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long productId;

    /** 数量 */
    @Excel(name = "数量")
    private Integer quantity;

    /** 是否选中（0否 1是） */
    @Excel(name = "选中", readConverterExp = "0=否,1=是")
    private String selected;

    public void setCartItemId(Long cartItemId)
    {
        this.cartItemId = cartItemId;
    }

    public Long getCartItemId()
    {
        return cartItemId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public Integer getQuantity()
    {
        return quantity;
    }

    public void setQuantity(Integer quantity)
    {
        this.quantity = quantity;
    }

    public String getSelected()
    {
        return selected;
    }

    public void setSelected(String selected)
    {
        this.selected = selected;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("cartItemId", getCartItemId())
            .append("userId", getUserId())
            .append("productId", getProductId())
            .append("quantity", getQuantity())
            .append("selected", getSelected())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
