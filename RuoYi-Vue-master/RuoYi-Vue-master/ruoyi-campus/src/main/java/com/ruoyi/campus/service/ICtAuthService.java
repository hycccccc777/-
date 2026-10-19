package com.ruoyi.campus.service;

import java.util.Map;
import com.ruoyi.campus.domain.CtLoginBody;

/**
 * 手机号认证 服务层
 * 
 * @author campus
 */
public interface ICtAuthService
{
    /**
     * 手机号 + 密码登录
     * 
     * @param body 登录请求
     * @return 含 token、expiresIn、user 的响应数据
     */
    public Map<String, Object> login(CtLoginBody body);

    /**
     * 获取当前登录用户信息
     * 
     * @return 用户信息
     */
    public Map<String, Object> currentUser();

    /**
     * 退出登录，使当前令牌失效
     */
    public void logout();
}
