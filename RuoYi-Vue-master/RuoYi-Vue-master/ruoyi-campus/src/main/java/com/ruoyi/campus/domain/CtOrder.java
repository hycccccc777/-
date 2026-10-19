package com.ruoyi.campus.domain;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 订单对象 ct_order
 * 
 * @author campus
 */
public class CtOrder extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 订单ID */
    private Long orderId;

    /** 订单号（业务唯一） */
    @Excel(name = "订单号")
    private String orderNo;

    /** 买家用户ID */
    @Excel(name = "买家ID")
    private Long buyerId;

    /** 卖家用户ID */
    @Excel(name = "卖家ID")
    private Long sellerId;

    /** 下单批次号 */
    private String groupNo;

    /** 商品总额（元） */
    @Excel(name = "商品总额")
    private BigDecimal totalAmount;

    /** 配送费（元） */
    @Excel(name = "配送费")
    private BigDecimal deliveryFee;

    /** 应付金额（元） */
    @Excel(name = "应付金额")
    private BigDecimal payAmount;

    /** 状态（0待付款 1待发货 2待收货 3已完成 4已取消 5退款中 6已退款） */
    @Excel(name = "状态", readConverterExp = "0=待付款,1=待发货,2=待收货,3=已完成,4=已取消,5=退款中,6=已退款")
    private String status;

    /** 配送方式（0自提 1配送） */
    @Excel(name = "配送方式", readConverterExp = "0=自提,1=配送")
    private String deliveryType;

    /** 收货人快照 */
    private String receiver;

    /** 收货电话快照 */
    private String receiverPhone;

    /** 收货地址快照 */
    private String addressDetail;

    /** 下单幂等键 */
    private String idempotencyKey;

    /** 支付时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date payTime;

    /** 发货时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date shipTime;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;

    /** 取消时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date cancelTime;

    /** 取消原因 */
    private String cancelReason;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    /** 订单明细集合（非表字段） */
    private List<CtOrderItem> orderItems;

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setBuyerId(Long buyerId)
    {
        this.buyerId = buyerId;
    }

    public Long getBuyerId()
    {
        return buyerId;
    }

    public void setSellerId(Long sellerId)
    {
        this.sellerId = sellerId;
    }

    public Long getSellerId()
    {
        return sellerId;
    }

    public String getGroupNo()
    {
        return groupNo;
    }

    public void setGroupNo(String groupNo)
    {
        this.groupNo = groupNo;
    }

    public BigDecimal getTotalAmount()
    {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount)
    {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDeliveryFee()
    {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal deliveryFee)
    {
        this.deliveryFee = deliveryFee;
    }

    public BigDecimal getPayAmount()
    {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount)
    {
        this.payAmount = payAmount;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDeliveryType()
    {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType)
    {
        this.deliveryType = deliveryType;
    }

    public String getReceiver()
    {
        return receiver;
    }

    public void setReceiver(String receiver)
    {
        this.receiver = receiver;
    }

    public String getReceiverPhone()
    {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone)
    {
        this.receiverPhone = receiverPhone;
    }

    public String getAddressDetail()
    {
        return addressDetail;
    }

    public void setAddressDetail(String addressDetail)
    {
        this.addressDetail = addressDetail;
    }

    public String getIdempotencyKey()
    {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey)
    {
        this.idempotencyKey = idempotencyKey;
    }

    public Date getPayTime()
    {
        return payTime;
    }

    public void setPayTime(Date payTime)
    {
        this.payTime = payTime;
    }

    public Date getShipTime()
    {
        return shipTime;
    }

    public void setShipTime(Date shipTime)
    {
        this.shipTime = shipTime;
    }

    public Date getFinishTime()
    {
        return finishTime;
    }

    public void setFinishTime(Date finishTime)
    {
        this.finishTime = finishTime;
    }

    public Date getCancelTime()
    {
        return cancelTime;
    }

    public void setCancelTime(Date cancelTime)
    {
        this.cancelTime = cancelTime;
    }

    public String getCancelReason()
    {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason)
    {
        this.cancelReason = cancelReason;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public List<CtOrderItem> getOrderItems()
    {
        return orderItems;
    }

    public void setOrderItems(List<CtOrderItem> orderItems)
    {
        this.orderItems = orderItems;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("orderId", getOrderId())
            .append("orderNo", getOrderNo())
            .append("buyerId", getBuyerId())
            .append("sellerId", getSellerId())
            .append("groupNo", getGroupNo())
            .append("totalAmount", getTotalAmount())
            .append("deliveryFee", getDeliveryFee())
            .append("payAmount", getPayAmount())
            .append("status", getStatus())
            .append("deliveryType", getDeliveryType())
            .append("receiver", getReceiver())
            .append("receiverPhone", getReceiverPhone())
            .append("addressDetail", getAddressDetail())
            .append("idempotencyKey", getIdempotencyKey())
            .append("payTime", getPayTime())
            .append("shipTime", getShipTime())
            .append("finishTime", getFinishTime())
            .append("cancelTime", getCancelTime())
            .append("cancelReason", getCancelReason())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
