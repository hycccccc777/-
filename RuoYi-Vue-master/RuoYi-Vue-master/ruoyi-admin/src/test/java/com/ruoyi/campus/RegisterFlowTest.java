package com.ruoyi.campus;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.domain.CtLoginBody;
import com.ruoyi.campus.domain.CtRegisterBody;
import com.ruoyi.campus.service.ICtAuthService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手机号自助注册。
 *
 * <p>验证的是"能注册"之外的三件事：注册后能不能直接登录、重复手机号能不能挡住、
 * 弱密码/非法手机号能不能挡住。注册会写 sys_user，测试用 1390000xxxx 号段，
 * 结束后统一清理，避免污染 seed 数据。
 */
@DisplayName("手机号注册")
class RegisterFlowTest extends CampusTestSupport
{
    @Autowired
    private ICtAuthService authService;

    @AfterEach
    void cleanupAfter()
    {
        cleanupRegisteredUsers();
    }

    @BeforeEach
    void cleanupBefore()
    {
        // 上一次异常中断可能残留测试账号，先清一遍保证可重复执行
        cleanupRegisteredUsers();
    }

    private void cleanupRegisteredUsers()
    {
        jdbc.update("delete from sys_user_role where user_id in (select user_id from sys_user where phonenumber like '1390000%')");
        jdbc.update("delete from sys_user where phonenumber like '1390000%'");
    }

    @Test
    @DisplayName("注册成功：写入用户、密码加密、可直接登录")
    void 注册成功后可直接登录()
    {
        String phone = "13900000001";
        Map<String, Object> data = authService.register(body(phone, "secret123", "小明"));

        assertNotNull(data.get("token"), "注册应直接返回访问令牌");
        assertNotNull(data.get("refreshToken"), "注册应返回刷新令牌");
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) data.get("user");
        assertEquals(phone, user.get("phone"));
        assertEquals("小明", user.get("nickname"));

        String password = jdbc.queryForObject(
                "select password from sys_user where phonenumber = ? and del_flag = '0'", String.class, phone);
        assertTrue(password.startsWith("$2"), "密码必须是 BCrypt 散列，不能明文落库");
        String status = jdbc.queryForObject(
                "select status from sys_user where phonenumber = ? and del_flag = '0'", String.class, phone);
        assertEquals("0", status, "新用户状态应为正常");

        CtLoginBody loginBody = new CtLoginBody();
        loginBody.setPhone(phone);
        loginBody.setPassword("secret123");
        assertNotNull(authService.login(loginBody).get("token"), "注册后应能用同一手机号密码登录");
    }

    @Test
    @DisplayName("昵称缺省用手机号后四位")
    void 昵称缺省用手机号后四位()
    {
        Map<String, Object> data = authService.register(body("13900000002", "secret123", null));
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) data.get("user");
        assertEquals("用户0002", user.get("nickname"));
    }

    @Test
    @DisplayName("重复手机号被拒绝（409），不会产生第二个账号")
    void 重复手机号被拒绝()
    {
        String phone = "13900000003";
        authService.register(body(phone, "secret123", "第一个"));

        ApiException e = assertThrows(ApiException.class,
                () -> authService.register(body(phone, "another123", "第二个")));
        assertEquals(409, e.getCode());

        Integer count = jdbc.queryForObject(
                "select count(*) from sys_user where phonenumber = ? and del_flag = '0'", Integer.class, phone);
        assertEquals(1, count, "同一手机号只能有一个账号");
    }

    @Test
    @DisplayName("非法手机号与弱密码被拒绝（400）")
    void 非法输入被拒绝()
    {
        ApiException badPhone = assertThrows(ApiException.class,
                () -> authService.register(body("12345", "secret123", "x")));
        assertEquals(400, badPhone.getCode());

        ApiException weakPwd = assertThrows(ApiException.class,
                () -> authService.register(body("13900000004", "123", "x")));
        assertEquals(400, weakPwd.getCode());

        Integer count = jdbc.queryForObject(
                "select count(*) from sys_user where phonenumber like '13900000004'", Integer.class);
        assertEquals(0, count, "校验失败不应写入任何用户");
    }

    private CtRegisterBody body(String phone, String password, String nickname)
    {
        CtRegisterBody b = new CtRegisterBody();
        b.setPhone(phone);
        b.setPassword(password);
        b.setNickname(nickname);
        return b;
    }
}
