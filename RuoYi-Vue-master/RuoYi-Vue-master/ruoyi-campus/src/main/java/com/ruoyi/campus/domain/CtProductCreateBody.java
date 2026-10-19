package com.ruoyi.campus.domain;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 发布商品请求体（/api/v1/products）。
 *
 * @author campus
 */
public class CtProductCreateBody
{
    /** 标题 */
    @NotBlank(message = "请填写商品标题")
    @Size(min = 4, max = 128, message = "标题长度需要在 4～128 个字符之间")
    private String title;

    /** 类目ID */
    @NotNull(message = "请选择商品类目")
    private Long categoryId;

    /** 售价 */
    @NotNull(message = "请填写售价")
    @DecimalMin(value = "0.01", message = "售价不能低于 0.01 元")
    @DecimalMax(value = "99999", message = "售价不能高于 99999 元")
    private BigDecimal price;

    /** 原价（可空） */
    @DecimalMin(value = "0.01", message = "原价不能低于 0.01 元")
    @DecimalMax(value = "99999", message = "原价不能高于 99999 元")
    private BigDecimal originalPrice;

    /** 库存（默认 1） */
    @Min(value = 0, message = "库存不能为负数")
    @Max(value = 9999, message = "库存不能超过 9999")
    private Integer stock;

    /** 成色编码（almost_new/nine/eight） */
    @Pattern(regexp = "almost_new|nine|eight", message = "成色只能是 almost_new / nine / eight")
    private String condition;

    /** 描述 */
    @Size(max = 2000, message = "描述不能超过 2000 个字符")
    private String description;

    /** 交接地点 */
    @Size(max = 100, message = "交接地点不能超过 100 个字符")
    private String location;

    /** 标签 */
    @Size(max = 32, message = "标签不能超过 32 个字符")
    private String tag;

    /** 图片地址列表（来自 /api/v1/upload） */
    @Size(max = 6, message = "最多上传 6 张图片")
    private List<String> images;

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

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

    public String getCondition()
    {
        return condition;
    }

    public void setCondition(String condition)
    {
        this.condition = condition;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getTag()
    {
        return tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public List<String> getImages()
    {
        return images;
    }

    public void setImages(List<String> images)
    {
        this.images = images;
    }
}
