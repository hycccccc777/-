package com.ruoyi.campus.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 把当前登录用户 ID 写入请求属性。
 *
 * <p>为什么不直接在 {@link ApiTraceFilter} 里读 SecurityUtils：
 * 该过滤器优先级最高，位于 Spring Security 之前，进入时尚未认证；
 * 而它的 finally 执行时认证上下文已被 Security 清理，两个时机都拿不到用户。
 * 拦截器在 DispatcherServlet 内执行，此时认证已完成，因此在这里取更可靠。
 *
 * @author campus
 */
@Component
public class RequestUserInterceptor implements HandlerInterceptor
{
    /** 请求属性名，供访问日志读取 */
    public static final String ATTR_USER_ID = "campus.requestUser";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
    {
        request.setAttribute(ATTR_USER_ID, resolveUserId());
        return true;
    }

    private String resolveUserId()
    {
        try
        {
            long userId = SecurityUtils.getUserId();
            return userId > 0 ? String.valueOf(userId) : "anonymous";
        }
        catch (Exception e)
        {
            return "anonymous";
        }
    }
}