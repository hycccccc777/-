package com.ruoyi.campus.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 支付单对象 ct_payment
 * 
 * @author campus
 */
public class CtPayment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 支付ID */
    private Long paymentId;

    /** 支付单号（业务唯一） */
    @Excel(name = "支付单号")
    private String payNo;

    /** 订单号 */
    @Excel(name = "订单号")
    private String orderNo;

    /** 支付用户ID */
    @Excel(name = "支付用户ID")
    private Long userId;

    /** 支付金额（元） */
    @Excel(name = "支付金额")
    private BigDecimal amount;

    /** 支付方式（0沙箱 1支付宝 2微信 3余额） */
    @Excel(name = "支付方式", readConverterExp = "0=沙箱,1=支付宝,2=微信,3=余额")
    private String payMethod;

    /** 状态（0待支付 1支付成功 2支付失败 3已退款） */
    @Excel(name = "状态", readConverterExp = "0=待支付,1=支付成功,2=支付失败,3=已退款")
    private String status;

    /** 第三方交易号 */
    @Excel(name = "第三方交易号")
    private String thirdPartyNo;

    /** 回调时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date callbackTime;

    public void setPaymentId(Long paymentId)
    {
        this.paymentId = paymentId;
    }

    public Long getPaymentId()
    {
        return paymentId;
    }

    public void setPayNo(String payNo)
    {
        this.payNo = payNo;
    }

    public String getPayNo()
    {
        return payNo;
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

    public BigDecimal getAmount()
    {
        return amount;
    }

    public void setAmount(BigDecimal amount)
    {
        this.amount = amount;
    }

    public String getPayMethod()
    {
        return payMethod;
    }

    public void setPayMethod(String payMethod)
    {
        this.payMethod = payMethod;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getThirdPartyNo()
    {
        return thirdPartyNo;
    }

    public void setThirdPartyNo(String thirdPartyNo)
    {
        this.thirdPartyNo = thirdPartyNo;
    }

    public Date getCallbackTime()
    {
        return callbackTime;
    }

    public void setCallbackTime(Date callbackTime)
    {
        this.callbackTime = callbackTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("paymentId", getPaymentId())
            .append("payNo", getPayNo())
            .append("orderNo", getOrderNo())
            .append("userId", getUserId())
            .append("amount", getAmount())
            .append("payMethod", getPayMethod())
            .append("status", getStatus())
            .append("thirdPartyNo", getThirdPartyNo())
            .append("callbackTime", getCallbackTime())
            .append("createTime", getCreateTime())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
