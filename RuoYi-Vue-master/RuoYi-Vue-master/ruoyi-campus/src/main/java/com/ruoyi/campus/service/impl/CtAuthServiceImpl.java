package com.ruoyi.campus.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.auth.RefreshTokenService;
import com.ruoyi.campus.domain.CtLoginBody;
import com.ruoyi.campus.mapper.CtAuthMapper;
import com.ruoyi.campus.service.ICtAuthService;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.UserStatus;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.framework.web.service.UserDetailsServiceImpl;
import com.ruoyi.system.service.ISysUserService;

/**
 * 手机号认证 服务层实现
 * 
 * @author campus
 */
@Service
public class CtAuthServiceImpl implements ICtAuthService
{
    @Autowired
    private CtAuthMapper ctAuthMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    /** 令牌有效期（分钟），与若依 token.expireTime 保持一致 */
    @Value("${token.expireTime}")
    private int expireTime;

    @Override
    public Map<String, Object> login(CtLoginBody body)
    {
        String phone = body == null ? null : StringUtils.trim(body.getPhone());
        String password = body == null ? null : body.getPassword();
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(password))
        {
            throw new ApiException(400, "手机号和密码不能为空");
        }

        SysUser basic = ctAuthMapper.selectUserByPhone(phone);
        if (basic == null)
        {
            throw new ApiException(401, "手机号或密码错误");
        }

        // 重新按用户名加载完整用户（含角色），供权限构建使用
        SysUser user = userService.selectUserByUserName(basic.getUserName());
        if (user == null)
        {
            throw new ApiException(401, "手机号或密码错误");
        }
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            throw new ApiException(401, "账号已被删除");
        }
        if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            throw new ApiException(401, "账号已停用，请联系管理员");
        }
        if (!SecurityUtils.matchesPassword(password, user.getPassword()))
        {
            throw new ApiException(401, "手机号或密码错误");
        }

        LoginUser loginUser = (LoginUser) userDetailsService.createLoginUser(user);
        String token = tokenService.createToken(loginUser);

        userService.updateLoginInfo(user.getUserId(), IpUtils.getIpAddr(), DateUtils.getNowDate());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("refreshToken", refreshTokenService.issue(user.getUserId()));
        data.put("expiresIn", expireTime * 60);
        data.put("user", toUserMap(user));
        return data;
    }

    @Override
    public Map<String, Object> refresh(String refreshToken)
    {
        // 先消费（并作废）刷新令牌，再签发新令牌
        Long userId = refreshTokenService.consume(refreshToken);

        SysUser user = userService.selectUserById(userId);
        if (user == null)
        {
            throw new ApiException(401, "用户不存在，请重新登录");
        }
        if (UserStatus.DELETED.getCode().equals(user.getDelFlag())
                || UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            // 账号在登录之后被停用/删除时，刷新同样必须拒绝
            throw new ApiException(401, "账号不可用，请重新登录");
        }

        LoginUser loginUser = (LoginUser) userDetailsService.createLoginUser(user);
        String token = tokenService.createToken(loginUser);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("refreshToken", refreshTokenService.issue(userId));
        data.put("expiresIn", expireTime * 60);
        data.put("user", toUserMap(user));
        return data;
    }

    @Override
    public Map<String, Object> currentUser()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return toUserMap(loginUser.getUser());
    }

    @Override
    public void logout()
    {
        try
        {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null && StringUtils.isNotEmpty(loginUser.getToken()))
            {
                tokenService.delLoginUser(loginUser.getToken());
            }
        }
        catch (Exception ignored)
        {
            // 令牌已失效时直接视为退出成功
        }
    }

    private Map<String, Object> toUserMap(SysUser user)
    {
        List<String> roles = new ArrayList<>();
        if (user.getRoles() != null)
        {
            for (SysRole role : user.getRoles())
            {
                if (StringUtils.isNotEmpty(role.getRoleKey()))
                {
                    roles.add(role.getRoleKey());
                }
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", String.valueOf(user.getUserId()));
        m.put("username", user.getUserName());
        m.put("nickname", user.getNickName());
        m.put("name", user.getNickName());
        m.put("avatar", user.getAvatar() == null ? "" : user.getAvatar());
        m.put("phone", user.getPhonenumber());
        m.put("roles", roles);
        m.put("role", roles.isEmpty() ? "user" : roles.get(0));
        m.put("admin", user.isAdmin());
        return m;
    }
}
