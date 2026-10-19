package com.ruoyi.campus.domain;

/**
 * 创建支付单请求体（/api/v1/pay/create）。
 *
 * @author campus
 */
public class CtPayBody
{
    /** 订单号 */
    private String orderNo;

    /** 支付方式（0沙箱 1支付宝 2微信 3余额） */
    private String payMethod;

    public String getOrderNo()
    {
        return orderNo;
    }

    public void setOrderNo(String orderNo)
    {
        this.orderNo = orderNo;
    }

    public String getPayMethod()
    {
        return payMethod;
    }

    public void setPayMethod(String payMethod)
    {
        this.payMethod = payMethod;
    }
}