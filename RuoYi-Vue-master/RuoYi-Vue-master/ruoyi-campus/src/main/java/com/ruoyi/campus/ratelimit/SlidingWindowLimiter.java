package com.ruoyi.campus.ratelimit;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * 滑动窗口限流器。
 *
 * <p>选滑动窗口而非固定窗口，是因为固定窗口存在临界点双倍流量问题：
 * 限 60/分钟 时，用户在 59 秒和 61 秒各发 30 个，实际一分钟内放过 60 个，
 * 恰好卡在边界时能瞬时打穿。
 *
 * <p>两种后端：
 * <ul>
 *   <li>memory（默认）：单实例部署，进程内 Deque，零依赖</li>
 *   <li>redis：多实例部署，ZSET + Lua 保证跨实例原子计数</li>
 * </ul>
 * 通过 {@code campus.ratelimit.store} 切换，对外接口不变。
 *
 * <p>为什么多实例必须用 redis：进程内计数在多副本下会被放大 N 倍
 * （N = 副本数），限流形同虚设。
 *
 * @author campus
 */
@Component
public class SlidingWindowLimiter
{
    private static final Logger log = LoggerFactory.getLogger(SlidingWindowLimiter.class);

    /** 滑动窗口：清理过期成员 -> 计数 -> 未超限则写入当前请求 */
    private static final String SLIDING_WINDOW_LUA = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local permits = tonumber(ARGV[3])
            local member = ARGV[4]
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            local count = redis.call('ZCARD', key)
            if count >= permits then
                return 0
            end
            redis.call('ZADD', key, now, member)
            redis.call('PEXPIRE', key, window)
            return 1
            """;

    private static final DefaultRedisScript<Long> SCRIPT =
            new DefaultRedisScript<>(SLIDING_WINDOW_LUA, Long.class);

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    private final AtomicLong requestCount = new AtomicLong();

    @Value("${campus.ratelimit.enabled:true}")
    private boolean enabled;

    /** memory | redis；未注入 Spring 时（单元测试 new 出来）保持 memory */
    private String store = "memory";

    private StringRedisTemplate redisTemplate;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getStore()
    {
        return store;
    }

    @Value("${campus.ratelimit.store:memory}")
    public void setStore(String store)
    {
        this.store = (store == null || store.isBlank()) ? "memory" : store.trim().toLowerCase();
    }

    @Autowired(required = false)
    public void setRedisTemplate(StringRedisTemplate redisTemplate)
    {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取一个配额。
     *
     * @return true=放行；false=超出配额
     */
    public boolean tryAcquire(String key, int permits, long windowMillis)
    {
        if (!enabled)
        {
            return true;
        }
        if (useRedis())
        {
            try
            {
                return tryAcquireRedis(key, permits, windowMillis);
            }
            catch (RuntimeException e)
            {
                // 限流是保护手段，不应因 Redis 抖动把业务打死：降级为放行并留痕
                log.warn("ratelimit redis unavailable, fail-open key={} err={}", key, e.getMessage());
                return true;
            }
        }
        return tryAcquireMemory(key, permits, windowMillis);
    }

    private boolean useRedis()
    {
        return "redis".equals(store) && redisTemplate != null;
    }

    private boolean tryAcquireRedis(String key, int permits, long windowMillis)
    {
        String member = System.currentTimeMillis() + ":" + UUID.randomUUID().toString().substring(0, 8);
        List<String> keys = Collections.singletonList("campus:ratelimit:" + key);
        Long allowed = redisTemplate.execute(SCRIPT, keys,
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(windowMillis),
                String.valueOf(permits),
                member);
        return allowed != null && allowed == 1L;
    }

    private boolean tryAcquireMemory(String key, int permits, long windowMillis)
    {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;

        Deque<Long> hits = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (hits)
        {
            while (!hits.isEmpty() && hits.peekFirst() < windowStart)
            {
                hits.pollFirst();
            }
            if (hits.size() >= permits)
            {
                return false;
            }
            hits.addLast(now);
        }

        // 每 1000 次请求做一次机会性清理，避免空闲 key 长期堆积占内存
        if (requestCount.incrementAndGet() % 1000 == 0)
        {
            evictIdle(windowStart);
        }
        return true;
    }

    public int trackedKeys()
    {
        return windows.size();
    }

    public void reset()
    {
        windows.clear();
        if (useRedis())
        {
            // 测试便利：清理本限流器有前缀的键
            try
            {
                var keys = redisTemplate.keys("campus:ratelimit:*");
                if (keys != null && !keys.isEmpty())
                {
                    redisTemplate.delete(keys);
                }
            }
            catch (RuntimeException e)
            {
                log.debug("reset redis keys failed: {}", e.getMessage());
            }
        }
    }

    private void evictIdle(long windowStart)
    {
        windows.entrySet().removeIf(e ->
        {
            Deque<Long> hits = e.getValue();
            synchronized (hits)
            {
                while (!hits.isEmpty() && hits.peekFirst() < windowStart)
                {
                    hits.pollFirst();
                }
                return hits.isEmpty();
            }
        });
    }
}
