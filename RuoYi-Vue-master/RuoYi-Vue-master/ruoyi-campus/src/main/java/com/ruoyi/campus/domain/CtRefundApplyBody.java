package com.ruoyi.campus.domain;

/**
 * 申请退款请求体。
 *
 * @author campus
 */
public class CtRefundApplyBody
{
    /** 退款原因 */
    private String reason;

    /** 卖家拒绝原因（拒绝时使用） */
    private String remark;

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}