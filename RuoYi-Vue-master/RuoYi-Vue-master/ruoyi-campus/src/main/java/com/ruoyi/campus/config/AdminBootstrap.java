package com.ruoyi.campus.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysUserService;

/**
 * 首次上线初始化管理员密码。
 *
 * <p>很多人上线时忘了改若依自带的 admin/admin123。这里在启动时，只有满足
 * “提供了 CAMPUS_ADMIN_PASSWORD 且 admin 仍是默认密码”才把 admin 密码重置为
 * 配置值，其它情况一律跳过——因此不会在每次重启时改密码，也不会覆盖已改过的密码。
 *
 * <p>需要强制重置时设置 CAMPUS_ADMIN_FORCE_RESET=true。
 *
 * @author campus
 */
@Component
public class AdminBootstrap implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private static final String ADMIN_USERNAME = "admin";

    private static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    @Autowired
    private ISysUserService userService;

    @Value("${campus.admin.password:}")
    private String initPassword;

    @Value("${campus.admin.force-reset:false}")
    private boolean forceReset;

    @Override
    public void run(ApplicationArguments args)
    {
        if (initPassword == null || initPassword.isBlank())
        {
            return;
        }
        if (initPassword.length() < 8)
        {
            log.warn("CAMPUS_ADMIN_PASSWORD 长度不足 8 位，已忽略管理员密码初始化");
            return;
        }
        SysUser admin = userService.selectUserByUserName(ADMIN_USERNAME);
        if (admin == null)
        {
            log.warn("未找到 admin 账号，跳过管理员密码初始化");
            return;
        }
        boolean stillDefault = SecurityUtils.matchesPassword(DEFAULT_ADMIN_PASSWORD, admin.getPassword());
        if (!stillDefault && !forceReset)
        {
            log.info("admin 密码已不是默认值，跳过初始化（如需强制重置请设 CAMPUS_ADMIN_FORCE_RESET=true）");
            return;
        }
        userService.resetUserPwd(admin.getUserId(), SecurityUtils.encryptPassword(initPassword));
        log.info("已初始化管理员 admin 密码（来源 CAMPUS_ADMIN_PASSWORD），请妥善保管");
    }
}
