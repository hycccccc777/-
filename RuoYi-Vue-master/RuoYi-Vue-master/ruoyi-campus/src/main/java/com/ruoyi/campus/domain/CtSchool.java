package com.ruoyi.campus.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 学校对象 ct_school
 * 
 * @author campus
 */
public class CtSchool extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 学校ID */
    private Long schoolId;

    /** 学校全称 */
    @Excel(name = "学校全称")
    private String schoolName;

    /** 学校简称 */
    @Excel(name = "学校简称")
    private String shortName;

    /** 所在城市 */
    @Excel(name = "所在城市")
    private String city;

    /** 显示顺序 */
    @Excel(name = "显示顺序")
    private Integer orderNum;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    public void setSchoolId(Long schoolId)
    {
        this.schoolId = schoolId;
    }

    public Long getSchoolId()
    {
        return schoolId;
    }

    @NotBlank(message = "学校全称不能为空")
    @Size(min = 0, max = 64, message = "学校全称长度不能超过64个字符")
    public String getSchoolName()
    {
        return schoolName;
    }

    public void setSchoolName(String schoolName)
    {
        this.schoolName = schoolName;
    }

    public String getShortName()
    {
        return shortName;
    }

    public void setShortName(String shortName)
    {
        this.shortName = shortName;
    }

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public Integer getOrderNum()
    {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
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
            .append("schoolId", getSchoolId())
            .append("schoolName", getSchoolName())
            .append("shortName", getShortName())
            .append("city", getCity())
            .append("orderNum", getOrderNum())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
