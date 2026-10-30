package com.ruoyi.campus.domain;

/**
 * 商品审核请求体。
 *
 * @author campus
 */
public class CtProductAuditBody
{
    /** true=通过并上架，false=驳回 */
    private boolean approve;

    /** 驳回原因，通过时忽略 */
    private String remark;

    public boolean isApprove()
    {
        return approve;
    }

    public void setApprove(boolean approve)
    {
        this.approve = approve;
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
