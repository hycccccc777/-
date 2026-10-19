package com.ruoyi.campus.mapper;

import com.ruoyi.common.core.domain.entity.SysUser;

/**
 * 手机号认证 数据层
 * 
 * @author campus
 */
public interface CtAuthMapper
{
    /**
     * 根据手机号查询用户（仅返回认证所需基础字段）
     * 
     * @param phonenumber 手机号
     * @return 用户对象信息
     */
    public SysUser selectUserByPhone(String phonenumber);
}
