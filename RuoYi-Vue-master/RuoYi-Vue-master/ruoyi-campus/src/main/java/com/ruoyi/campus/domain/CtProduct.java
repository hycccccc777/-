package com.ruoyi.campus.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 商品对象 ct_product
 * 
 * @author campus
 */
public class CtProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 商品ID */
    private Long productId;

    /** 商品标题 */
    @Excel(name = "商品标题")
    private String title;

    /** 类目ID */
    @Excel(name = "类目ID")
    private Long categoryId;

    /** 卖家用户ID */
    @Excel(name = "卖家ID")
    private Long sellerId;

    /** 卖家类型（0个人 1商家） */
    @Excel(name = "卖家类型", readConverterExp = "0=个人,1=商家")
    private String sellerType;

    /** 售价（元） */
    @Excel(name = "售价")
    private BigDecimal price;

    /** 原价（元） */
    @Excel(name = "原价")
    private BigDecimal originalPrice;

    /** 库存 */
    @Excel(name = "库存")
    private Integer stock;

    /** 成色（字典 ct_condition） */
    @Excel(name = "成色")
    private String conditionCode;

    /** 所属学校ID */
    @Excel(name = "学校ID")
    private Long schoolId;

    /** 交接地点 */
    @Excel(name = "交接地点")
    private String location;

    /** 封面图URL */
    private String coverUrl;

    /** 标签 */
    @Excel(name = "标签")
    private String tag;

    /** 商品描述 */
    private String description;

    /** 状态（0待审核 1已上架 2已下架 3审核驳回 4已售出） */
    @Excel(name = "状态", readConverterExp = "0=待审核,1=已上架,2=已下架,3=审核驳回,4=已售出")
    private String status;

    /** 审核意见 */
    private String auditRemark;

    /** 浏览量 */
    private Integer viewCount;

    /** 收藏数 */
    private Integer favoriteCount;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getProductId()
    {
        return productId;
    }

    @NotBlank(message = "商品标题不能为空")
    @Size(min = 0, max = 128, message = "商品标题长度不能超过128个字符")
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @NotNull(message = "类目ID不能为空")
    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public Long getSellerId()
    {
        return sellerId;
    }

    public void setSellerId(Long sellerId)
    {
        this.sellerId = sellerId;
    }

    public String getSellerType()
    {
        return sellerType;
    }

    public void setSellerType(String sellerType)
    {
        this.sellerType = sellerType;
    }

    @NotNull(message = "售价不能为空")
    public BigDecimal getPrice()
    {
        return price;
    }

    public void setPrice(BigDecimal price)
    {
        this.price = price;
    }

    public BigDecimal getOriginalPrice()
    {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice)
    {
        this.originalPrice = originalPrice;
    }

    public Integer getStock()
    {
        return stock;
    }

    public void setStock(Integer stock)
    {
        this.stock = stock;
    }

    public String getConditionCode()
    {
        return conditionCode;
    }

    public void setConditionCode(String conditionCode)
    {
        this.conditionCode = conditionCode;
    }

    public Long getSchoolId()
    {
        return schoolId;
    }

    public void setSchoolId(Long schoolId)
    {
        this.schoolId = schoolId;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getCoverUrl()
    {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl)
    {
        this.coverUrl = coverUrl;
    }

    public String getTag()
    {
        return tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getAuditRemark()
    {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark)
    {
        this.auditRemark = auditRemark;
    }

    public Integer getViewCount()
    {
        return viewCount;
    }

    public void setViewCount(Integer viewCount)
    {
        this.viewCount = viewCount;
    }

    public Integer getFavoriteCount()
    {
        return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount)
    {
        this.favoriteCount = favoriteCount;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("productId", getProductId())
            .append("title", getTitle())
            .append("categoryId", getCategoryId())
            .append("sellerId", getSellerId())
            .append("sellerType", getSellerType())
            .append("price", getPrice())
            .append("originalPrice", getOriginalPrice())
            .append("stock", getStock())
            .append("conditionCode", getConditionCode())
            .append("schoolId", getSchoolId())
            .append("location", getLocation())
            .append("coverUrl", getCoverUrl())
            .append("tag", getTag())
            .append("description", getDescription())
            .append("status", getStatus())
            .append("auditRemark", getAuditRemark())
            .append("viewCount", getViewCount())
            .append("favoriteCount", getFavoriteCount())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
