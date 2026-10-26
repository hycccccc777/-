package com.ruoyi.campus.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 框架级异常在 /api/v1 上的语义测试。
 *
 * <p>锁定的是一类很难通过"看一眼代码"发现的缺陷：错误发生在请求还没落到
 * Controller 的阶段，若依的 GlobalExceptionHandler 会把它包成
 * HTTP 200 + body code 500，于是"方法用错""参数传错"在下游看来是成功请求，
 * 还会把客户端错误算进服务端 5xx 指标。
 *
 * <p>这些用例直接调用 advice 的重写方法，不启动完整容器，保证快速且稳定。
 *
 * @author campus
 */
class ApiMvcExceptionAdviceTest
{
    private final ApiMvcExceptionAdvice advice = new ApiMvcExceptionAdvice();

    private static ServletWebRequest req(String uri, String httpMethod)
    {
        MockHttpServletRequest request = new MockHttpServletRequest(httpMethod, uri);
        request.setRequestURI(uri);
        return new ServletWebRequest(request);
    }

    private static MethodParameter dummyParameter() throws NoSuchMethodException
    {
        Method m = ApiMvcExceptionAdviceTest.class.getDeclaredMethod("sample", String.class);
        return new MethodParameter(m, 0);
    }

    @SuppressWarnings("unused")
    private void sample(String value)
    {
    }

    private static Map<String, Object> body(ResponseEntity<Object> entity)
    {
        assertNotNull(entity.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) entity.getBody();
        return map;
    }

    @Test
    @DisplayName("方法不支持：/api/v1 返回 405 且符合前端契约")
    void methodNotSupportedReturns405() throws Exception
    {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<Object> entity = advice.handleHttpRequestMethodNotSupported(
                ex, new HttpHeaders(), HttpStatus.METHOD_NOT_ALLOWED, req("/api/v1/products", "DELETE"));

        assertEquals(405, entity.getStatusCode().value());
        assertEquals(405, body(entity).get("code"));
        assertTrue(body(entity).get("message").toString().contains("DELETE"));
    }

    @Test
    @DisplayName("参数类型不匹配：/api/v1 返回 400，不污染 5xx 指标")
    void typeMismatchReturns400() throws Exception
    {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", dummyParameter(), new NumberFormatException("x"));

        ResponseEntity<Object> entity = advice.handleTypeMismatch(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, req("/api/v1/products/abc", "GET"));

        assertEquals(400, entity.getStatusCode().value());
        assertEquals(400, body(entity).get("code"));
        assertTrue(body(entity).get("message").toString().contains("id"));
    }

    @Test
    @DisplayName("请求体校验失败：/api/v1 返回 400 且是项目响应体而不是 problem+json")
    void invalidBodyUsesProjectContract() throws Exception
    {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "body");
        binding.addError(new FieldError("body", "cartItemIds", "不能为空"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(dummyParameter(), binding);

        ResponseEntity<Object> entity = advice.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, req("/api/v1/orders", "POST"));

        assertEquals(400, entity.getStatusCode().value());
        assertEquals(400, body(entity).get("code"));
        assertTrue(body(entity).get("message").toString().contains("不能为空"));
    }

    @Test
    @DisplayName("非法 JSON：/api/v1 返回 400 统一响应体")
    void unreadableBodyReturns400() throws Exception
    {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "bad json", new MockHttpInputMessage("not-json".getBytes()));

        ResponseEntity<Object> entity = advice.handleHttpMessageNotReadable(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, req("/api/v1/orders", "POST"));

        assertEquals(400, entity.getStatusCode().value());
        assertEquals(400, body(entity).get("code"));
    }

    @Test
    @DisplayName("非 /api/v1 路径维持若依原有行为：HTTP 200 + AjaxResult")
    void nonApiPathKeepsLegacyBehavior() throws Exception
    {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<Object> entity = advice.handleHttpRequestMethodNotSupported(
                ex, new HttpHeaders(), HttpStatus.METHOD_NOT_ALLOWED, req("/ruoyi/system/user", "DELETE"));

        // 若依后台前端按 body 里的 code 判断，所以这里必须仍是 200 + AjaxResult
        assertEquals(200, entity.getStatusCode().value());
        assertTrue(entity.getBody() instanceof com.ruoyi.common.core.domain.AjaxResult);
        assertEquals(500, ((com.ruoyi.common.core.domain.AjaxResult) entity.getBody()).get("code"));
    }
}
