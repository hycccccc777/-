package com.ruoyi.campus.observability;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * /api/v1 的链路追踪与结构化访问日志入口。
 *
 * <p>做三件事：
 * <ol>
 *   <li>生成或沿用 traceId，写入上下文与响应头（前端报错时可原样回报，便于对账）</li>
 *   <li>把 traceId 放进 MDC，日志格式里用 %X{traceId} 输出</li>
 *   <li>每个请求打一行结构化访问日志，便于机器采集与排障</li>
 * </ol>
 *
 * @author campus
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiTraceFilter extends OncePerRequestFilter
{
    /** 单独的 logger 分类，logback 里可独立配置级别与输出 */
    private static final Logger accessLog = LoggerFactory.getLogger("API_ACCESS");

    private static final String PATH_PREFIX = "/api/v1";

    /** 只处理前端接口；/ruoyi/** 后台与静态资源不产生噪音日志 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        // 上游（网关或前端重试）已带 traceId 就沿用，保证跨服务链路不断
        String traceId = TraceContext.sanitize(request.getHeader(TraceContext.HEADER));
        if (traceId == null)
        {
            traceId = TraceContext.newId();
        }
        TraceContext.set(traceId);
        response.setHeader(TraceContext.HEADER, traceId);

        long startNanos = System.nanoTime();
        int status = 200;
        try
        {
            chain.doFilter(request, response);
            status = response.getStatus();
        }
        catch (IOException | ServletException | RuntimeException e)
        {
            status = 500;
            throw e;
        }
        finally
        {
            long costMs = (System.nanoTime() - startNanos) / 1_000_000;
            accessLog.info("api method={} path={} status={} costMs={} userId={}", request.getMethod(),
                    request.getRequestURI(), status, costMs, currentUserId());
            // 必须清理：线程池复用会把 traceId 带到下一个请求
            TraceContext.clear();
        }
    }

    /** 未登录时返回 0；SecurityUtils 在无认证上下文下会抛异常，这里不能让它影响主流程 */
    private String currentUserId()
    {
        try
        {
            return String.valueOf(SecurityUtils.getUserId());
        }
        catch (Exception e)
        {
            return "anonymous";
        }
    }
}