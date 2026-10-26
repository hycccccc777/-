package com.ruoyi.campus.domain;

/**
 * 手机号注册请求体。
 *
 * @author campus
 */
public class CtRegisterBody
{
    /** 手机号，作为登录账号 */
    private String phone;

    /** 昵称，可空，默认取手机号后四位 */
    private String nickname;

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

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
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
