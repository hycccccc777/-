package com.ruoyi.campus.domain;

import java.util.Date;

/**
 * 下单幂等记录 ct_idempotency
 *
 * @author campus
 */
public class CtIdempotency
{
    /** 幂等ID */
    private Long idemId;

    /** 客户端幂等键（唯一索引 uk_ct_idem_key） */
    private String idemKey;

    /** 下单批次号，创建完成后回写 */
    private String groupNo;

    /** 状态（0处理中 1已完成） */
    private String status;

    /** 创建时间 */
    private Date createTime;

    public Long getIdemId()
    {
        return idemId;
    }

    public void setIdemId(Long idemId)
    {
        this.idemId = idemId;
    }

    public String getIdemKey()
    {
        return idemKey;
    }

    public void setIdemKey(String idemKey)
    {
        this.idemKey = idemKey;
    }

    public String getGroupNo()
    {
        return groupNo;
    }

    public void setGroupNo(String groupNo)
    {
        this.groupNo = groupNo;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
