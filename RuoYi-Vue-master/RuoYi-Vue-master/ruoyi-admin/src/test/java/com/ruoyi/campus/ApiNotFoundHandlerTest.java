package com.ruoyi.campus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.ruoyi.campus.api.ApiNotFoundHandler;

/**
 * 未知路径的 404 修正测试。
 *
 * <p>若依默认把「路径不存在」也包成 HTTP 200 + code 500，
 * 前端打错路径时拿到的是成功状态码，监控与缓存都会误判。
 * 这里锁定修正后的行为：/api/v1 返回真正的 404 且符合前端契约。
 *
 * @author campus
 */
class ApiNotFoundHandlerTest
{
    private final ApiNotFoundHandler handler = new ApiNotFoundHandler();

    private static NoResourceFoundException notFound(String uri)
    {
        return new NoResourceFoundException(HttpMethod.GET, uri, uri);
    }

    @Test
    @DisplayName("/api/v1 下的未知路径返回 HTTP 404")
    void apiPathReturnsReal404()
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/not-exist");
        request.setRequestURI("/api/v1/not-exist");

        Object result = handler.handle(notFound("/api/v1/not-exist"), request);

        ResponseEntity<?> entity = assertInstanceOf(ResponseEntity.class, result);
        assertEquals(404, entity.getStatusCode().value());
    }

    @Test
    @DisplayName("404 响应体符合前端契约 {code, message, data}")
    void bodyUsesFrontendContract()
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/not-exist");
        request.setRequestURI("/api/v1/not-exist");

        ResponseEntity<?> entity = (ResponseEntity<?>) handler.handle(notFound("/api/v1/not-exist"), request);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entity.getBody();
        assertEquals(404, body.get("code"));
        assertEquals(true, body.get("message").toString().contains("/api/v1/not-exist"));
    }

    @Test
    @DisplayName("非前端路径维持若依原有行为（不改变后台管理端的错误结构）")
    void otherPathsKeepRuoYiBehavior()
    {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ruoyi/not-exist");
        request.setRequestURI("/ruoyi/not-exist");

        Object result = handler.handle(notFound("/ruoyi/not-exist"), request);

        // 若依返回 AjaxResult，不是 ResponseEntity
        assertEquals(false, result instanceof ResponseEntity);
    }
}