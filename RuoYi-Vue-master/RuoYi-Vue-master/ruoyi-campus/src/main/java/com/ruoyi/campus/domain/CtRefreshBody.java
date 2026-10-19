package com.ruoyi.campus.domain;

import jakarta.validation.constraints.NotBlank;
import com.ruoyi.common.xss.Xss;

/**
 * 刷新令牌请求体
 *
 * @author campus
 */
public class CtRefreshBody
{
    @NotBlank(message = "刷新令牌不能为空")
    @Xss
    private String refreshToken;

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken)
    {
        this.refreshToken = refreshToken;
    }
}