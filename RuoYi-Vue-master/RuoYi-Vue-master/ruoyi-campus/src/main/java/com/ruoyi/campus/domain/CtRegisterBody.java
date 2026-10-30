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

    /** 短信验证码（开启短信验证时必填） */
    private String smsCode;

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

    public String getSmsCode()
    {
        return smsCode;
    }

    public void setSmsCode(String smsCode)
    {
        this.smsCode = smsCode;
    }
}
