package com.ruoyi.campus.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 用户校园档案对象 ct_user_profile
 * 
 * @author campus
 */
public class CtUserProfile extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 档案ID */
    private Long profileId;

    /** 用户ID（sys_user.user_id） */
    @Excel(name = "用户ID")
    private Long userId;

    /** 真实姓名 */
    @Excel(name = "真实姓名")
    private String realName;

    /** 学号 */
    @Excel(name = "学号")
    private String studentNo;

    /** 所属学校ID */
    @Excel(name = "学校ID")
    private Long schoolId;

    /** 学号认证状态（0未认证 1待审核 2已认证 3已驳回） */
    @Excel(name = "认证状态", readConverterExp = "0=未认证,1=待审核,2=已认证,3=已驳回")
    private String verifyStatus;

    /** 认证备注/驳回原因 */
    private String verifyRemark;

    /** 认证通过时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "认证时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date verifyTime;

    /** 常用联系方式 */
    @Excel(name = "联系方式")
    private String contactPhone;

    public void setProfileId(Long profileId)
    {
        this.profileId = profileId;
    }

    public Long getProfileId()
    {
        return profileId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public String getRealName()
    {
        return realName;
    }

    public void setRealName(String realName)
    {
        this.realName = realName;
    }

    public String getStudentNo()
    {
        return studentNo;
    }

    public void setStudentNo(String studentNo)
    {
        this.studentNo = studentNo;
    }

    public Long getSchoolId()
    {
        return schoolId;
    }

    public void setSchoolId(Long schoolId)
    {
        this.schoolId = schoolId;
    }

    public String getVerifyStatus()
    {
        return verifyStatus;
    }

    public void setVerifyStatus(String verifyStatus)
    {
        this.verifyStatus = verifyStatus;
    }

    public String getVerifyRemark()
    {
        return verifyRemark;
    }

    public void setVerifyRemark(String verifyRemark)
    {
        this.verifyRemark = verifyRemark;
    }

    public Date getVerifyTime()
    {
        return verifyTime;
    }

    public void setVerifyTime(Date verifyTime)
    {
        this.verifyTime = verifyTime;
    }

    public String getContactPhone()
    {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone)
    {
        this.contactPhone = contactPhone;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("profileId", getProfileId())
            .append("userId", getUserId())
            .append("realName", getRealName())
            .append("studentNo", getStudentNo())
            .append("schoolId", getSchoolId())
            .append("verifyStatus", getVerifyStatus())
            .append("verifyRemark", getVerifyRemark())
            .append("verifyTime", getVerifyTime())
            .append("contactPhone", getContactPhone())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .toString();
    }
}
