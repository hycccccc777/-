package com.ruoyi.campus.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品图片对象 ct_product_image
 * 
 * @author campus
 */
public class CtProductImage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 图片ID */
    private Long imageId;

    /** 商品ID */
    @Excel(name = "商品ID")
    private Long productId;

    /** 图片URL */
    @Excel(name = "图片URL")
    private String imageUrl;

    /** 排序 */
    @Excel(name = "排序")
    private Integer orderNum;

    public void setImageId(Long imageId)
    {
        this.imageId = imageId;
    }

    public Long getImageId()
    {
        return imageId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("imageId", getImageId())
            .append("productId", getProductId())
            .append("imageUrl", getImageUrl())
            .append("orderNum", getOrderNum())
            .append("createTime", getCreateTime())
            .toString();
    }
}
