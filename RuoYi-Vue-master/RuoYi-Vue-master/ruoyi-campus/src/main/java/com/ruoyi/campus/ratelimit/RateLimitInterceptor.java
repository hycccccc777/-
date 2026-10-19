package com.ruoyi.campus.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 读取 {@link RateLimit} 注解并执行限流。
 *
 * <p>用 HandlerInterceptor 而不是 AOP：限流要回答的是「进不进得去」，
 * 应该在进入 Controller 之前就拒绝；AOP 切在方法上会先完成参数绑定和校验，
 * 恶意刷接口时这部分反序列化开销纯属浪费。
 *
 * @author campus
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor
{
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final SlidingWindowLimiter limiter;

    public RateLimitInterceptor(SlidingWindowLimiter limiter)
    {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        if (!(handler instanceof HandlerMethod))
        {
            return true;
        }
        RateLimit rule = ((HandlerMethod) handler).getMethodAnnotation(RateLimit.class);
        if (rule == null)
        {
            return true;
        }

        String dimension;
        if (rule.scope() == RateLimit.Scope.IP)
        {
            dimension = "ip:" + clientIp(request);
        }
        else
        {
            String userId = currentUserId(request);
            // 未登录时若仍按用户维度，所有匿名访问会共用一个配额，
            // 表现就是「随机有人被限流」，因此自动退化为按 IP。
            dimension = "0".equals(userId) ? "ip:" + clientIp(request) : "u:" + userId;
        }
        String key = rule.key() + "|" + dimension;

        if (!limiter.tryAcquire(key, rule.permits(), rule.seconds() * 1000L))
        {
            log.warn("ratelimit rejected rule={} dimension={} limit={}/{}s", rule.key(), dimension, rule.permits(),
                    rule.seconds());
            // 429 语义正确：调用方应退避重试，而不是继续猛打
            throw new ApiException(429, "请求过于频繁，请稍后再试");
        }
        return true;
    }

    /** 未登录时返回 0，由调用方退化为按 IP 限流 */
    private String currentUserId(HttpServletRequest request)
    {
        try
        {
            long userId = SecurityUtils.getUserId();
            return userId > 0 ? String.valueOf(userId) : "0";
        }
        catch (Exception e)
        {
            return "0";
        }
    }

    /**
     * 取真实客户端 IP。
     *
     * <p>经过反向代理后 getRemoteAddr 是代理的地址，所以优先读
     * X-Forwarded-For 的第一段。取不到就退回 remoteAddr——但要注意，
     * 此时所有用户会共享同一个配额，这比不生效更糟，因此调用方需要
     * 结合是否登录来决定降级维度。
     */
    private String clientIp(HttpServletRequest request)
    {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank())
        {
            // 形如 "client, proxy1, proxy2"，第一段才是真实客户端
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank())
        {
            return realIp.trim();
        }
        String remote = request.getRemoteAddr();
        return (remote == null || remote.isBlank()) ? "unknown" : remote;
    }
}