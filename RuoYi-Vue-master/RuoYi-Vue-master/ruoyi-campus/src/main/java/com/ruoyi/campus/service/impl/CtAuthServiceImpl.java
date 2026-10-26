package com.ruoyi.campus.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.auth.RefreshTokenService;
import com.ruoyi.campus.domain.CtLoginBody;
import com.ruoyi.campus.domain.CtRegisterBody;
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
import com.ruoyi.common.utils.uuid.IdUtils;
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

    /** 是否开放自助注册：对外演示或生产可按需关闭 */
    @Value("${campus.auth.register-enabled:true}")
    private boolean registerEnabled;

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

    /**
     * 手机号自助注册。
     *
     * <p>与若依自带的 /register 区别：本接口以手机号为账号，注册成功直接返回登录态，
     * 且不依赖图形验证码（前端是移动端风格的手机号 + 密码，没有验证码环节）。
     * 因此这里用 IP 限流 + 手机号唯一约束来控制刷注册。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> register(CtRegisterBody body)
    {
        if (!registerEnabled)
        {
            throw new ApiException(403, "当前未开放注册，请联系管理员");
        }
        String phone = body == null ? null : StringUtils.trim(body.getPhone());
        String password = body == null ? null : body.getPassword();
        String nickname = body == null ? null : StringUtils.trim(body.getNickname());
        if (StringUtils.isEmpty(phone) || StringUtils.isEmpty(password))
        {
            throw new ApiException(400, "手机号和密码不能为空");
        }
        if (!phone.matches("^1[3-9]\\d{9}$"))
        {
            throw new ApiException(400, "请输入有效的 11 位手机号");
        }
        if (password.length() < 6 || password.length() > 20)
        {
            throw new ApiException(400, "密码长度需为 6 到 20 个字符");
        }
        if (StringUtils.isEmpty(nickname))
        {
            nickname = "用户" + phone.substring(phone.length() - 4);
        }
        if (nickname.length() > 20)
        {
            throw new ApiException(400, "昵称不能超过 20 个字符");
        }
        if (ctAuthMapper.selectUserByPhone(phone) != null)
        {
            throw new ApiException(409, "该手机号已注册，请直接登录");
        }

        SysUser user = new SysUser();
        user.setUserName("u" + phone);
        if (!userService.checkUserNameUnique(user))
        {
            // 极少数情况下手机号被用作过别的用户名，追加随机后缀避免唯一键冲突
            user.setUserName("u" + phone + IdUtils.fastSimpleUUID().substring(0, 4));
        }
        user.setNickName(nickname);
        user.setPhonenumber(phone);
        user.setPassword(SecurityUtils.encryptPassword(password));
        user.setStatus(UserStatus.OK.getCode());
        user.setPwdUpdateDate(DateUtils.getNowDate());
        user.setCreateBy("register");

        boolean created = userService.registerUser(user);
        if (!created || user.getUserId() == null)
        {
            throw new ApiException(500, "注册失败，请稍后重试");
        }

        SysUser saved = userService.selectUserByUserName(user.getUserName());
        LoginUser loginUser = (LoginUser) userDetailsService.createLoginUser(saved);
        String token = tokenService.createToken(loginUser);
        userService.updateLoginInfo(saved.getUserId(), IpUtils.getIpAddr(), DateUtils.getNowDate());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("refreshToken", refreshTokenService.issue(saved.getUserId()));
        data.put("expiresIn", expireTime * 60);
        data.put("user", toUserMap(saved));
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
