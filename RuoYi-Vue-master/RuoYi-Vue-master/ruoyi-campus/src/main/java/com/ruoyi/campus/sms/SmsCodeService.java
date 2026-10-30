package com.ruoyi.campus.sms;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.StringUtils;

/**
 * 短信验证码服务：生成、限频、落 Redis、校验。
 *
 * <p>限频分两层：同一手机同一场景的发送间隔（防连点），
 * 以及单手机号的每日上限（防刷短信、控成本）。
 *
 * @author campus
 */
@Service
public class SmsCodeService
{
    private static final String CODE_PREFIX = "campus:sms:code:";

    private static final String INTERVAL_PREFIX = "campus:sms:interval:";

    private static final String DAILY_PREFIX = "campus:sms:daily:";

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private SmsSender smsSender;

    @Value("${campus.sms.enabled:false}")
    private boolean enabled;

    @Value("${campus.sms.code-ttl-seconds:300}")
    private int codeTtlSeconds;

    @Value("${campus.sms.send-interval-seconds:60}")
    private int sendIntervalSeconds;

    @Value("${campus.sms.daily-limit:10}")
    private int dailyLimit;

    public boolean isEnabled()
    {
        return enabled;
    }

    public String provider()
    {
        return smsSender.provider();
    }

    /**
     * 发送验证码。同一手机号+场景在间隔期内只能发一次，单号每日有上限。
     */
    public void send(String phone, String scene)
    {
        if (!enabled)
        {
            throw new ApiException(400, "短信验证当前未开启");
        }
        String normalized = scene == null ? "register" : scene.trim().toLowerCase();
        if (!"register".equals(normalized) && !"login".equals(normalized) && !"reset".equals(normalized))
        {
            throw new ApiException(400, "不支持的验证场景");
        }
        String intervalKey = INTERVAL_PREFIX + normalized + ":" + phone;
        if (Boolean.TRUE.equals(redisCache.hasKey(intervalKey)))
        {
            throw new ApiException(429, "发送过于频繁，请稍后再试");
        }
        String dailyKey = DAILY_PREFIX + phone;
        Long count = redisCache.redisTemplate.opsForValue().increment(dailyKey);
        if (count != null && count == 1L)
        {
            redisCache.expire(dailyKey, 1, TimeUnit.DAYS);
        }
        if (count != null && count > dailyLimit)
        {
            throw new ApiException(429, "今日验证码发送次数已达上限");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisCache.setCacheObject(CODE_PREFIX + normalized + ":" + phone, code, codeTtlSeconds, TimeUnit.SECONDS);
        redisCache.setCacheObject(intervalKey, "1", sendIntervalSeconds, TimeUnit.SECONDS);
        smsSender.sendCode(phone, normalized, code);
    }

    /**
     * 校验验证码，成功后立即失效（一次性）。
     * SMS 关闭时不校验，直接放行，保证旧的注册/登录用例不受影响。
     */
    public void verify(String phone, String scene, String code)
    {
        if (!enabled)
        {
            return;
        }
        if (StringUtils.isEmpty(code))
        {
            throw new ApiException(400, "请输入短信验证码");
        }
        String normalized = scene == null ? "register" : scene.trim().toLowerCase();
        String key = CODE_PREFIX + normalized + ":" + phone;
        Object stored = redisCache.getCacheObject(key);
        if (stored == null)
        {
            throw new ApiException(400, "验证码已过期，请重新获取");
        }
        if (!String.valueOf(stored).equals(code.trim()))
        {
            throw new ApiException(400, "验证码不正确");
        }
        redisCache.deleteObject(key);
    }
}
