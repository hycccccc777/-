package com.ruoyi.campus.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 退款/售后对象 ct_refund
 *
 * @author campus
 */
public class CtRefund extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 退款ID */
    private Long refundId;

    /** 退款单号 */
    private String refundNo;

    /** 订单ID */
    private Long orderId;

    /** 订单号 */
    private String orderNo;

    /** 申请人（买家） */
    private Long userId;

    /** 卖家用户ID */
    private Long sellerId;

    /** 退款原因 */
    private String reason;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 状态（0待审核 1已同意 2已拒绝 3已退款完成） */
    private String status;

    /** 申请前订单状态 */
    private String orderStatusBefore;

    /** 处理人 */
    private String handleBy;

    /** 处理时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handleTime;

    /** 处理说明 */
    private String handleRemark;

    /** 删除标志 */
    private String delFlag;

    public void setRefundId(Long refundId)
    {
        this.refundId = refundId;
    }

    public Long getRefundId()
    {
        return refundId;
    }

    public void setRefundNo(String refundNo)
    {
        this.refundNo = refundNo;
    }

    public String getRefundNo()
    {
        return refundNo;
    }

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

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setSellerId(Long sellerId)
    {
        this.sellerId = sellerId;
    }

    public Long getSellerId()
    {
        return sellerId;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getReason()
    {
        return reason;
    }

    public void setRefundAmount(BigDecimal refundAmount)
    {
        this.refundAmount = refundAmount;
    }

    public BigDecimal getRefundAmount()
    {
        return refundAmount;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return status;
    }

    public void setOrderStatusBefore(String orderStatusBefore)
    {
        this.orderStatusBefore = orderStatusBefore;
    }

    public String getOrderStatusBefore()
    {
        return orderStatusBefore;
    }

    public void setHandleBy(String handleBy)
    {
        this.handleBy = handleBy;
    }

    public String getHandleBy()
    {
        return handleBy;
    }

    public void setHandleTime(Date handleTime)
    {
        this.handleTime = handleTime;
    }

    public Date getHandleTime()
    {
        return handleTime;
    }

    public void setHandleRemark(String handleRemark)
    {
        this.handleRemark = handleRemark;
    }

    public String getHandleRemark()
    {
        return handleRemark;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("refundId", getRefundId())
            .append("refundNo", getRefundNo())
            .append("orderNo", getOrderNo())
            .append("userId", getUserId())
            .append("sellerId", getSellerId())
            .append("reason", getReason())
            .append("refundAmount", getRefundAmount())
            .append("status", getStatus())
            .append("orderStatusBefore", getOrderStatusBefore())
            .append("handleBy", getHandleBy())
            .append("handleTime", getHandleTime())
            .append("handleRemark", getHandleRemark())
            .toString();
    }
}