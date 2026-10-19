package com.ruoyi.campus.domain;

/**
 * 手机号登录请求体。
 *
 * @author campus
 */
public class CtLoginBody
{
    /** 手机号 */
    private String phone;

    /** 密码 */
    private String password;

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}
