package com.ruoyi.campus.auth;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.common.core.redis.RedisCache;

/**
 * 刷新令牌服务。
 *
 * <p>存 Redis 而不是内存：多实例部署时刷新请求可能落到任意节点，
 * 内存实现会导致「换个节点就刷新失败」。
 *
 * <p>采用一次性刷新：每次刷新作废旧 refreshToken 并下发新的。
 * 好处是刷新令牌一旦泄露，攻击者用过之后真实用户再刷新会失败，能察觉异常；
 * 代价是并发刷新需要前端串行化。
 *
 * @author campus
 */
@Service
public class RefreshTokenService
{
    private static final String KEY_PREFIX = "campus:refresh:";

    /** 刷新令牌最短有效长度，低于此值直接判定非法 */
    private static final int MIN_LENGTH = 16;

    @Autowired
    private RedisCache redisCache;

    /** 刷新令牌有效期（天） */
    @Value("${campus.auth.refresh-token-days:7}")
    private int refreshTokenDays;

    /**
     * 为指定用户签发刷新令牌。
     */
    public String issue(Long userId)
    {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisCache.setCacheObject(KEY_PREFIX + token, String.valueOf(userId), refreshTokenDays, TimeUnit.DAYS);
        return token;
    }

    /**
     * 校验刷新令牌并解析用户 ID，同时作废该令牌。
     *
     * @throws ApiException 令牌无效、已过期或已被使用
     */
    public Long consume(String refreshToken)
    {
        if (refreshToken == null || refreshToken.length() < MIN_LENGTH)
        {
            throw new ApiException(401, "刷新令牌无效");
        }
        String key = KEY_PREFIX + refreshToken;
        Object value = redisCache.getCacheObject(key);
        if (value == null)
        {
            // 一次性刷新：拿不到说明已被用过或已过期，两种情况都要求重新登录
            throw new ApiException(401, "刷新令牌已失效，请重新登录");
        }
        redisCache.deleteObject(key);
        try
        {
            return Long.valueOf(String.valueOf(value));
        }
        catch (NumberFormatException e)
        {
            throw new ApiException(401, "刷新令牌数据异常");
        }
    }

    /**
     * 退出登录时作废刷新令牌。
     */
    public void revoke(String refreshToken)
    {
        if (refreshToken != null && refreshToken.length() >= MIN_LENGTH)
        {
            redisCache.deleteObject(KEY_PREFIX + refreshToken);
        }
    }

    /** 刷新令牌剩余有效期（秒），随刷新结果返回给前端 */
    public int refreshTokenTtlSeconds()
    {
        return refreshTokenDays * 24 * 3600;
    }
}