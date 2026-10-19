package com.ruoyi.campus.controller;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.ruoyi.campus.api.ApiResp;

/**
 * /api/v1 接口的入参校验异常统一处理。
 *
 * <p>若依自带的 GlobalExceptionHandler 会把校验异常转成 {code, msg}，
 * 与我们约定的 {code, message, data} 不一致，因此这里优先拦截并统一格式，
 * 保证前端只需处理一套响应结构。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ruoyi.campus.controller")
public class ApiExceptionHandler
{
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** @RequestBody 上的 @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException e)
    {
        // 聚合全部提示：多个约束同时失败时只报第一条，会让用户反复试错
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining("；"));
        if (message.isEmpty())
        {
            message = "请求参数不合法";
        }
        log.debug("参数校验失败: {}", message);
        return ApiResp.fail(400, message);
    }

    /** 查询参数 / 表单绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBind(BindException e)
    {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("请求参数不合法");
        return ApiResp.fail(400, message);
    }

    /** 方法参数上的 @Validated 校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException e)
    {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("请求参数不合法");
        return ApiResp.fail(400, message);
    }

    /** 请求体无法解析（JSON 格式错误、类型不匹配等） */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadable(HttpMessageNotReadableException e)
    {
        log.debug("请求体解析失败: {}", e.getMessage());
        return ApiResp.fail(400, "请求体格式不正确");
    }
}