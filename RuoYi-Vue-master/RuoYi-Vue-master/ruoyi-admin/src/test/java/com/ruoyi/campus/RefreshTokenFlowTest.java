package com.ruoyi.campus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.auth.RefreshTokenService;

/**
 * 刷新令牌流程测试。
 *
 * <p>验证一次性刷新语义：成功消费后原令牌作废，复用必须失败。
 * 这是防刷新令牌泄露的核心机制。
 *
 * @author campus
 */
class RefreshTokenFlowTest extends CampusTestSupport
{
    @Autowired
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("签发→消费：返回签发时的用户 ID")
    void issueAndConsume()
    {
        String token = refreshTokenService.issue(2001L);
        assertNotNull(token);
        assertEquals(2001L, refreshTokenService.consume(token));
    }

    @Test
    @DisplayName("一次性语义：消费过的令牌复用必须失败")
    void consumedTokenCannotBeReused()
    {
        String token = refreshTokenService.issue(2001L);
        refreshTokenService.consume(token);

        ApiException e = assertThrows(ApiException.class, () -> refreshTokenService.consume(token));
        assertEquals(401, e.getCode());
    }

    @Test
    @DisplayName("非法格式直接拒绝，不触碰 Redis")
    void malformedTokenRejected()
    {
        ApiException e1 = assertThrows(ApiException.class, () -> refreshTokenService.consume(null));
        assertEquals(401, e1.getCode());

        ApiException e2 = assertThrows(ApiException.class, () -> refreshTokenService.consume("short"));
        assertEquals(401, e2.getCode());
    }

    @Test
    @DisplayName("不同用户拿到的令牌互不相同")
    void tokensAreUniquePerUser()
    {
        String a = refreshTokenService.issue(2001L);
        String b = refreshTokenService.issue(2002L);
        assertNotEquals(a, b);
        assertEquals(2001L, refreshTokenService.consume(a));
        assertEquals(2002L, refreshTokenService.consume(b));
    }
}