package com.ruoyi.campus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import com.ruoyi.campus.observability.TraceContext;
import com.ruoyi.campus.ratelimit.SlidingWindowLimiter;

/**
 * P3 可观测性与限流的单元测试。
 *
 * <p>这些都是纯逻辑组件，不需要 Spring 上下文，因此跑得很快（毫秒级），
 * 适合作为每次提交的快速回归。
 *
 * @author campus
 */
class ObservabilityTest
{
    @AfterEach
    void cleanUp()
    {
        TraceContext.clear();
    }

    @Test
    @DisplayName("traceId 未设置时返回占位符而不是 null")
    void traceIdFallsBackToPlaceholder()
    {
        TraceContext.clear();
        assertEquals("-", TraceContext.get());
    }

    @Test
    @DisplayName("traceId 同时写入 ThreadLocal 与 MDC")
    void traceIdIsWrittenToMdc()
    {
        TraceContext.set("abc123");
        assertEquals("abc123", TraceContext.get());
        assertEquals("abc123", MDC.get(TraceContext.MDC_KEY));
    }

    @Test
    @DisplayName("clear 后 MDC 不留残余，避免线程复用串味")
    void clearRemovesBoth()
    {
        TraceContext.set("xyz");
        TraceContext.clear();
        assertEquals("-", TraceContext.get());
        assertNull(MDC.get(TraceContext.MDC_KEY));
    }

    @Test
    @DisplayName("每次生成的 traceId 长度固定且互不相同")
    void newIdIsUniqueAndFixedLength()
    {
        String a = TraceContext.newId();
        String b = TraceContext.newId();
        assertEquals(16, a.length());
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("上游 traceId 清洗掉可注入日志的字符")
    void sanitizeStripsDangerousChars()
    {
        // 换行符能把伪造的日志行塞进日志文件，必须被剔除
        assertEquals("abc", TraceContext.sanitize("a\nbc"));
        assertEquals("trace-123", TraceContext.sanitize("trace-123"));
        // 尖括号被剔除后剩下的 script 本身无害，属合法 traceId
        assertEquals("script", TraceContext.sanitize("<script>"));
        // 只由非法字符组成时才判非法
        assertNull(TraceContext.sanitize("<<>>"));
        assertNull(TraceContext.sanitize(""));
        assertNull(TraceContext.sanitize(null));
        // 超长直接判非法，避免日志被撑爆
        assertNull(TraceContext.sanitize("a".repeat(33)));
    }

    @Test
    @DisplayName("限流窗口内超过配额即拒绝")
    void limiterRejectsBeyondQuota()
    {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter();
        limiter.setEnabled(true);

        for (int i = 0; i < 5; i++)
        {
            assertTrue(limiter.tryAcquire("k", 5, 60_000), "第 " + (i + 1) + " 次应放行");
        }
        assertFalse(limiter.tryAcquire("k", 5, 60_000), "第 6 次应被拒绝");
    }

    @Test
    @DisplayName("不同 key 互不影响，一个用户被打满不能连坐其他用户")
    void limiterIsolatesKeys()
    {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter();
        limiter.setEnabled(true);

        for (int i = 0; i < 3; i++)
        {
            limiter.tryAcquire("userA", 3, 60_000);
        }
        assertFalse(limiter.tryAcquire("userA", 3, 60_000));
        assertTrue(limiter.tryAcquire("userB", 3, 60_000), "B 用户不应受 A 影响");
    }

    @Test
    @DisplayName("窗口滑过后配额恢复")
    void limiterRecoversAfterWindow() throws Exception
    {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter();
        limiter.setEnabled(true);

        assertTrue(limiter.tryAcquire("w", 1, 150));
        assertFalse(limiter.tryAcquire("w", 1, 150), "窗口内第二次应拒绝");
        Thread.sleep(200);
        assertTrue(limiter.tryAcquire("w", 1, 150), "窗口滑过后应恢复");
    }

    @Test
    @DisplayName("开关关闭时一律放行，便于压测与本地调试")
    void limiterDisabledAlwaysAllows()
    {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter();
        limiter.setEnabled(false);
        for (int i = 0; i < 100; i++)
        {
            assertTrue(limiter.tryAcquire("k", 1, 60_000));
        }
        assertEquals(0, limiter.trackedKeys(), "关闭时不应记录任何 key");
    }

    @Test
    @DisplayName("reset 清空所有窗口")
    void limiterResetClearsWindows()
    {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter();
        limiter.setEnabled(true);
        limiter.tryAcquire("a", 5, 60_000);
        limiter.tryAcquire("b", 5, 60_000);
        assertTrue(limiter.trackedKeys() >= 2);
        limiter.reset();
        assertEquals(0, limiter.trackedKeys());
    }
}