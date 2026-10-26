package com.ruoyi.campus.api;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import com.ruoyi.common.core.domain.AjaxResult;

/**
 * 处理在「请求还没落到 Controller」时抛出的框架级异常。
 *
 * <p>为什么不能放进 {@code ApiExceptionHandler}：
 * 那个类用 {@code @RestControllerAdvice(basePackages=...)} 限定作用范围，
 * 而方法和参数错误发生在 handler 解析阶段 —— 此时还没有匹配到任何 Controller，
 * 作用域判断失效，异常会落到若依的 {@code GlobalExceptionHandler}，
 * 最终表现为「HTTP 200 + body code 500」。
 *
 * <p>这会带来两个实际问题：
 * <ul>
 *   <li>网关、监控、前端拦截器都按 HTTP 状态判断成败，200 会被当成成功</li>
 *   <li>客户端传错参数被记成服务端 5xx，污染错误率指标</li>
 * </ul>
 *
 * <p>因此这里用不限定包路径的 advice，只在 /api/v1 前缀下改写状态码，
 * 其它路径（若依后台）保持原有行为，避免影响管理端。
 *
 * @author campus
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ApiMvcExceptionAdvice extends ResponseEntityExceptionHandler
{
    private static final String API_PREFIX = "/api/v1";

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request)
    {
        HttpServletRequest servletRequest = ((org.springframework.web.context.request.ServletWebRequest) request).getRequest();
        if (!isApi(servletRequest))
        {
            return legacy(AjaxResult.error(ex.getMessage()));
        }
        return wrap(ApiResp.fail(405, "请求方法不被支持：" + ex.getMethod()));
    }

    /**
     * 参数类型不匹配（如把非数字传给 Long 类型的 id）在上游是 400 语义，
     * 但默认会被包成 500。这里改为 400，避免把客户端错误算进服务端错误率。
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            org.springframework.beans.TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request)
    {
        HttpServletRequest servletRequest = ((org.springframework.web.context.request.ServletWebRequest) request).getRequest();
        if (!isApi(servletRequest))
        {
            return legacy(AjaxResult.error(ex.getMessage()));
        }
        // Spring 7 把该回调的参数放宽为 TypeMismatchException，具体名字/目标类型仍在子类型里
        String name = "参数";
        Class<?> required = null;
        if (ex instanceof MethodArgumentTypeMismatchException m)
        {
            name = m.getName();
            required = m.getRequiredType();
        }
        String type = required == null ? "目标类型" : required.getSimpleName();
        return wrap(ApiResp.fail(400, "参数[" + name + "]格式不正确，应为 " + type));
    }

    private boolean isApi(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        return uri != null && uri.startsWith(API_PREFIX);
    }

    /**
     * @Valid @RequestBody 校验失败。
     *
     * <p>必须在这里重新实现一遍，不能只依赖 campus 包内的 ApiExceptionHandler：
     * 本类优先级更高，而父类对 MethodArgumentNotValidException 有兜底处理，
     * 一旦落到父类就会渲染成 application/problem+json，破坏前端约定的响应结构。
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            org.springframework.web.bind.MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request)
    {
        HttpServletRequest servletRequest = ((org.springframework.web.context.request.ServletWebRequest) request).getRequest();
        if (!isApi(servletRequest))
        {
            return legacy(AjaxResult.error("请求参数不合法"));
        }
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.joining("；"));
        if (message.isEmpty())
        {
            message = "请求参数不合法";
        }
        return wrap(ApiResp.fail(400, message));
    }

    /** 缺少必填查询参数 */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            org.springframework.web.bind.MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request)
    {
        HttpServletRequest servletRequest = ((org.springframework.web.context.request.ServletWebRequest) request).getRequest();
        if (!isApi(servletRequest))
        {
            return legacy(AjaxResult.error("缺少必要参数：" + ex.getParameterName()));
        }
        return wrap(ApiResp.fail(400, "缺少必要参数：" + ex.getParameterName()));
    }

    /**
     * 请求体校验失败。
     *
     * <p>Spring 6.1+ 对方法级校验抛的是 HandlerMethodValidationException，
     * 默认渲染成 application/problem+json，与前端约定的 {code,message,data} 不一致。
     * 这里统一成项目响应体，避免前端要多写一套解析分支。
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            org.springframework.web.method.annotation.HandlerMethodValidationException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request)
    {
        HttpServletRequest servletRequest = ((org.springframework.web.context.request.ServletWebRequest) request).getRequest();
        if (!isApi(servletRequest))
        {
            return legacy(AjaxResult.error("请求参数不合法"));
        }
        return wrap(ApiResp.fail(400, "请求参数不合法"));
    }

    /** 请求体无法解析（JSON 语法错误等）同样统一成项目响应体 */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request)
    {
        HttpServletRequest servletRequest = ((org.springframework.web.context.request.ServletWebRequest) request).getRequest();
        if (!isApi(servletRequest))
        {
            return legacy(AjaxResult.error("请求体格式不正确"));
        }
        return wrap(ApiResp.fail(400, "请求体格式不正确"));
    }

    /**
     * 非 /api/v1 路径维持若依原有行为：把错误包成 AjaxResult 并返回 HTTP 200。
     *
     * <p>这一步不能省。父类的默认实现会返回 405/400 + problem+json，
     * 而若依后台前端是按 {code, msg} 判断的，直接改变状态码会让管理端报错。
     * 「只在 /api/v1 上纠正语义」是本次修复的边界。
     */
    private ResponseEntity<Object> legacy(AjaxResult result)
    {
        return ResponseEntity.ok(result);
    }

    /** 把 ResponseEntity&lt;Map&gt; 转成父类要求的 ResponseEntity&lt;Object&gt;，保留状态码与统一响应体 */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ResponseEntity<Object> wrap(ResponseEntity<Map<String, Object>> source)
    {
        return (ResponseEntity) source;
    }
}
