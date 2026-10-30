package com.ruoyi.campus.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本地日志短信发送器（默认实现）。
 *
 * <p>未配置短信服务商、或处于本地开发/测试环境时使用：只把验证码
 * 打到日志，不真正发短信。生产环境必须切换到真实服务商，否则用户收不到码。
 *
 * @author campus
 */
public class LogSmsSender implements SmsSender
{
    private static final Logger log = LoggerFactory.getLogger(LogSmsSender.class);

    @Override
    public void sendCode(String phone, String scene, String code)
    {
        log.info("[SMS-LOG] scene={} phone={} code={}（未配置短信服务商，仅打印）", scene, phone, code);
    }

    @Override
    public String provider()
    {
        return "log";
    }
}
