package com.ruoyi.campus.api;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * 未知路径的响应修正。
 *
 * <p>若依的全局异常处理器把所有异常都包成 {@code AjaxResult}（HTTP 200 + code 500），
 * 包括 Spring 抛出的 {@link NoResourceFoundException}。后果是：
 * 前端打错一个路径，拿到的是「HTTP 200 + 业务码 500」——
 * 监控、网关缓存、健康检查都会把它当成功，排障时也看不出是路径写错了。
 *
 * <p>这里只修正 /api/v1 前缀：返回真正的 404 与前端统一契约。
 * 其余路径（后台 /ruoyi/**、静态资源）维持若依原有行为，
 * 避免影响管理端前端对错误格式的既有依赖。
 *
 * @author campus
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiNotFoundHandler
{
    private static final Logger log = LoggerFactory.getLogger(ApiNotFoundHandler.class);

    private static final String API_PREFIX = "/api/v1";

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handle(NoResourceFoundException e, HttpServletRequest request)
    {
        String uri = request == null ? null : request.getRequestURI();
        if (uri != null && uri.startsWith(API_PREFIX))
        {
            log.debug("前端接口不存在 uri={}", uri);
            return ApiResp.fail(404, "接口不存在：" + uri);
        }
        // 非前端接口：保持若依原样，避免后台管理前端拿到不同的错误结构
        return AjaxResult.error("No static resource " + e.getResourcePath() + ".");
    }
}