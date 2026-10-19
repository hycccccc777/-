package com.ruoyi.campus.domain;

import java.util.List;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

/**
 * 下单请求体（/api/v1/orders）。
 *
 * @author campus
 */
public class CtOrderCreateBody
{
    /** 参与结算的购物车条目ID列表 */
    @NotEmpty(message = "请选择要结算的商品")
    private List<Long> cartItemIds;

    /** 收货地址ID（有地址管理接口后优先使用） */
    private Long addressId;

    /** 收货人（未传 addressId 时使用） */
    @Size(max = 20, message = "收货人不能超过 20 个字符")
    private String receiver;

    /** 收货电话（未传 addressId 时使用） */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入有效的 11 位手机号")
    private String phone;

    /** 收货地址详情（未传 addressId 时使用） */
    @Size(max = 200, message = "收货地址不能超过 200 个字符")
    private String addressDetail;

    /** 配送方式（0自提 1配送） */
    @Pattern(regexp = "^$|^[01]$", message = "配送方式只能是 0自提 或 1配送")
    private String deliveryType;

    /** 买家备注 */
    @Size(max = 200, message = "备注不能超过 200 个字符")
    private String remark;

    /** 幂等键：同一个键重复提交返回同一批订单 */
    @Size(max = 64, message = "幂等键不能超过 64 个字符")
    private String idempotencyKey;

    /**
     * 收货信息完整性：传了 addressId 就以地址簿为准，否则三项都必须填写。
     * 用 @AssertTrue 做跨字段的「二选一」校验，避免把条件判断写进 Service。
     */
    @AssertTrue(message = "请填写收货人、联系电话与收货地址")
    @JsonIgnore
    public boolean isContactComplete()
    {
        return addressId != null
                || (StringUtils.isNotBlank(receiver) && StringUtils.isNotBlank(phone) && StringUtils.isNotBlank(addressDetail));
    }

    public List<Long> getCartItemIds()
    {
        return cartItemIds;
    }

    public void setCartItemIds(List<Long> cartItemIds)
    {
        this.cartItemIds = cartItemIds;
    }

    public Long getAddressId()
    {
        return addressId;
    }

    public void setAddressId(Long addressId)
    {
        this.addressId = addressId;
    }

    public String getReceiver()
    {
        return receiver;
    }

    public void setReceiver(String receiver)
    {
        this.receiver = receiver;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getAddressDetail()
    {
        return addressDetail;
    }

    public void setAddressDetail(String addressDetail)
    {
        this.addressDetail = addressDetail;
    }

    public String getDeliveryType()
    {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType)
    {
        this.deliveryType = deliveryType;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public String getIdempotencyKey()
    {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey)
    {
        this.idempotencyKey = idempotencyKey;
    }
}
