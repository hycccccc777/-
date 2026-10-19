package com.ruoyi.campus.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 面向 /api/v1 前端的统一响应体：{"code":200,"message":"success","data":{}}
 * 注意：字段名是 message，与若依后台的 msg 不同。
 *
 * @author campus
 */
public final class ApiResp
{
    private ApiResp()
    {
    }

    public static ResponseEntity<Map<String, Object>> ok(Object data)
    {
        return build(HttpStatus.OK, 200, "success", data);
    }

    public static ResponseEntity<Map<String, Object>> fail(int code, String message)
    {
        return build(toHttpStatus(code), code, message, null);
    }

    private static ResponseEntity<Map<String, Object>> build(HttpStatus status, int code, String message, Object data)
    {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", data);
        return ResponseEntity.status(status)
                .header("X-Trace-Id", UUID.randomUUID().toString())
                .body(body);
    }

    private static HttpStatus toHttpStatus(int code)
    {
        HttpStatus status = HttpStatus.resolve(code);
        return status != null ? status : HttpStatus.BAD_REQUEST;
    }
}
