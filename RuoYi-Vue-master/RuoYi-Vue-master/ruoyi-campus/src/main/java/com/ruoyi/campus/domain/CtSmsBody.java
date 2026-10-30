package com.ruoyi.campus.domain;

/**
 * 发送短信验证码请求体（/api/v1/auth/sms/send）。
 *
 * @author campus
 */
public class CtSmsBody
{
    /** 手机号 */
    private String phone;

    /** 场景：register/login/reset，默认 register */
    private String scene;

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getScene()
    {
        return scene;
    }

    public void setScene(String scene)
    {
        this.scene = scene;
    }
}
