package com.ruoyi.campus.api;

/**
 * /api/v1 业务异常：携带业务码，由 ApiAuthController 统一转换为 ApiResp.fail。
 *
 * @author campus
 */
public class ApiException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /** 业务码，同时用作 HTTP 状态码（如 401、400、409） */
    private final int code;

    public ApiException(int code, String message)
    {
        super(message);
        this.code = code;
    }

    public ApiException(String message)
    {
        this(400, message);
    }

    public int getCode()
    {
        return code;
    }
}
