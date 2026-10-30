package com.ruoyi.campus.sms;

/**
 * 短信发送器。把「发一条验证码」与具体服务商解耦，
 * 切换阿里云/腾讯云/自建 = 换一个实现，业务层不动。
 *
 * @author campus
 */
public interface SmsSender
{
    /**
     * 发送验证码短信。实现应尽量吞掉异常并记录日志，
     * 是否要把失败透出给前端由调用方决定。
     *
     * @param phone 手机号（11 位，不含 +86）
     * @param scene 场景：register/login/reset
     * @param code  6 位数字验证码
     */
    void sendCode(String phone, String scene, String code);

    /** 服务商标识，便于日志与健康检查 */
    String provider();
}
