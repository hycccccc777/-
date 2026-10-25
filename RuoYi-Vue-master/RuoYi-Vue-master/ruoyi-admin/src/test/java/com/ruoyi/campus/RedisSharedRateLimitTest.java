package com.ruoyi.campus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ruoyi.campus.ratelimit.SlidingWindowLimiter;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 多实例限流共享状态验证。
 *
 * <p>单实例用进程内计数没问题；一旦水平扩到 N 个副本，每个副本各算各的，
 * 实际放行量会被放大 N 倍，限流等于没做。这里用两个独立的限流器实例
 * 指向同一个 Redis，验证配额是"全局共享"的，而不是各算各的。
 *
 * <p>对照组：两个内存实现，各自都能放满配额 —— 正是要避免的情况。
 *
 * @author campus
 */
class RedisSharedRateLimitTest extends CampusTestSupport
{
    private static final String PREFIX = "campus:ratelimit:";

    @Autowired
    private StringRedisTemplate redis;

    private SlidingWindowLimiter redisLimiter()
    {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter();
        limiter.setEnabled(true);
        limiter.setStore("redis");
        limiter.setRedisTemplate(redis);
        return limiter;
    }

    @Test
    @DisplayName("两个实例共享同一份配额：交替请求第 6 次必被拒")
    void quotaIsSharedAcrossInstances()
    {
        String key = "shared-" + System.nanoTime();
        SlidingWindowLimiter instanceA = redisLimiter();
        SlidingWindowLimiter instanceB = redisLimiter();

        try
        {
            // 交替打到两个"实例"上，共 5 次应全部放行
            for (int i = 0; i < 5; i++)
            {
                SlidingWindowLimiter target = (i % 2 == 0) ? instanceA : instanceB;
                assertTrue(target.tryAcquire(key, 5, 60_000), "第 " + (i + 1) + " 次应放行");
            }

            // 第 6 次无论落到哪个实例都必须拒绝 —— 这就是"共享"的含义
            assertFalse(instanceA.tryAcquire(key, 5, 60_000), "A 实例第 6 次应被拒绝");
            assertFalse(instanceB.tryAcquire(key, 5, 60_000), "B 实例第 6 次也应被拒绝");
        }
        finally
        {
            redis.delete(PREFIX + key);
        }
    }

    @Test
    @DisplayName("对照组：两个内存实例各算各的，配额被放大 2 倍")
    void memoryInstancesDoNotShare()
    {
        SlidingWindowLimiter memoryA = new SlidingWindowLimiter();
        memoryA.setEnabled(true);
        SlidingWindowLimiter memoryB = new SlidingWindowLimiter();
        memoryB.setEnabled(true);

        String key = "memory-" + System.nanoTime();
        for (int i = 0; i < 5; i++)
        {
            assertTrue(memoryA.tryAcquire(key, 5, 60_000));
            assertTrue(memoryB.tryAcquire(key, 5, 60_000));
        }

        // 两个实例各自都到了 5 次才拒绝 —— 合计放行 10 次，配额被放大
        assertFalse(memoryA.tryAcquire(key, 5, 60_000));
        assertFalse(memoryB.tryAcquire(key, 5, 60_000));
    }

    @Test
    @DisplayName("写进 Redis 的键带统一前缀并设置过期，不会泄漏")
    void redisKeyHasPrefixAndTtl()
    {
        String key = "ttl-" + System.nanoTime();
        SlidingWindowLimiter limiter = redisLimiter();

        try
        {
            assertTrue(limiter.tryAcquire(key, 10, 30_000));
            Long ttl = redis.getExpire(PREFIX + key);
            assertTrue(ttl != null && ttl > 0 && ttl <= 30, "TTL 应在 0~30 秒之间，实际=" + ttl);

            Set<String> keys = redis.keys(PREFIX + key);
            assertEquals(1, keys == null ? 0 : keys.size());
        }
        finally
        {
            redis.delete(PREFIX + key);
        }
    }
}
