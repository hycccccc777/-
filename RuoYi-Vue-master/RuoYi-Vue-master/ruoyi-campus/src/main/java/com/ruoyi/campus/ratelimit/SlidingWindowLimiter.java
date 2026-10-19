package com.ruoyi.campus.ratelimit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 内存滑动窗口限流器。
 *
 * <p>选滑动窗口而非固定窗口，是因为固定窗口存在临界点双倍流量问题：
 * 限 60/分钟 时，用户在 59 秒和 61 秒各发 30 个，实际一分钟内放过 60 个，
 * 恰好卡在边界时能瞬时打穿。
 *
 * <p>用内存而非 Redis：单实例部署足够，且不引入额外依赖；
 * 多实例部署时应换成 Redis 实现（Lua 保证原子性），对外接口不用变。
 *
 * @author campus
 */
@Component
public class SlidingWindowLimiter
{
    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    private final AtomicLong requestCount = new AtomicLong();

    @Value("${campus.ratelimit.enabled:true}")
    private boolean enabled;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
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