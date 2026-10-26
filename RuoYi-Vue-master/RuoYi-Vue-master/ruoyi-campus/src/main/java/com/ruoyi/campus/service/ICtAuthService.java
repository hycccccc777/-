package com.ruoyi.campus.service;

import java.util.Map;
import com.ruoyi.campus.domain.CtLoginBody;
import com.ruoyi.campus.domain.CtRegisterBody;

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
     * @return 含 token、refreshToken、expiresIn、user 的响应数据
     */
    public Map<String, Object> login(CtLoginBody body);

    /**
     * 手机号注册，成功后直接返回登录态（与 login 响应结构一致）。
     *
     * @param body 注册请求
     * @return 含 token、refreshToken、expiresIn、user 的响应数据
     */
    public Map<String, Object> register(CtRegisterBody body);

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

    /**
     * 用刷新令牌换取新的访问令牌。
     *
     * <p>刷新令牌是一次性的：成功后立即作废并下发新的。前端需保存新的
     * 刷新令牌，否则第二次刷新会失败。
     */
    public Map<String, Object> refresh(String refreshToken);
}