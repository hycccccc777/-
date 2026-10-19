package com.ruoyi.campus.observability;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * 链路追踪上下文。
 *
 * <p>traceId 在请求入口生成（或沿用上游传入的），写入 ThreadLocal 与 SLF4J MDC，
 * 于是同一请求里所有日志行都自动带上同一个 traceId，出问题时用
 * `grep <traceId> logs/sys-error.log` 就能捞出完整调用链。
 *
 * <p>请求结束后必须 {@link #clear()}，否则线程池复用会把上一个请求的
 * traceId 带到下一个请求上——这是异步场景下最常见的"日志串味"原因。
 *
 * @author campus
 */
public final class TraceContext
{
    /** 上下游约定的请求头 */
    public static final String HEADER = "X-Trace-Id";

    /** MDC 中的键名，logback 格式里用 %X{traceId} 引用 */
    public static final String MDC_KEY = "traceId";

    private static final int MAX_LENGTH = 32;

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceContext()
    {
    }

    public static void set(String traceId)
    {
        HOLDER.set(traceId);
        MDC.put(MDC_KEY, traceId);
    }

    /** 取当前 traceId，未设置时返回占位符而不是 null，避免日志里出现 "null" */
    public static String get()
    {
        String id = HOLDER.get();
        return id == null ? "-" : id;
    }

    public static void clear()
    {
        HOLDER.remove();
        MDC.remove(MDC_KEY);
    }

    public static String newId()
    {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 清洗上游传入的 traceId。
     *
     * <p>这个值会被写进日志文件，如果不做白名单过滤，攻击者可以借助
     * 请求头把换行符或伪造的日志行塞进日志文件（日志注入）。
     *
     * @return 合法则原样返回，非法返回 null 表示应重新生成
     */
    public static String sanitize(String candidate)
    {
        if (candidate == null)
        {
            return null;
        }
        String cleaned = candidate.replaceAll("[^A-Za-z0-9_-]", "");
        if (cleaned.isEmpty() || cleaned.length() > MAX_LENGTH)
        {
            return null;
        }
        return cleaned;
    }
}