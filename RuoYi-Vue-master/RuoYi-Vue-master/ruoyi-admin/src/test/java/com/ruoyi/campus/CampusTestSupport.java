package com.ruoyi.campus;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;

/**
 * 校园业务集成测试基类：使用独立 shop_test 库，不触碰开发库。
 *
 * <p>数据源通过 @SpringBootTest 内联属性指定。内联属性优先级高于 application*.yml，
 * 因此即使主配置里 spring.profiles.active=druid 也不会误连开发库 shop。
 */
@SpringBootTest(properties = {
        "spring.datasource.druid.master.url=jdbc:mysql://localhost:3306/shop_test?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=GMT%2B8",
        "spring.datasource.druid.master.username=root",
        "spring.datasource.druid.master.password=${CAMPUS_DB_PASSWORD:qazwsx@134567}",
        "spring.datasource.druid.maxActive=30",
        "spring.data.redis.database=4",
        "logging.level.root=warn",
        "logging.level.com.ruoyi=warn"
})
public abstract class CampusTestSupport
{
    protected static final Long SELLER_ID = 2100L;

    protected static final List<Long> BUYER_IDS = List.of(
            2001L, 2002L, 2003L, 2004L, 2005L, 2006L, 2007L, 2008L, 2009L, 2010L,
            2011L, 2012L, 2013L, 2014L, 2015L, 2016L, 2017L, 2018L, 2019L, 2020L);

    /** 库存为 1 的限量商品，用于复现超卖 */
    protected static final Long LIMIT_PRODUCT_ID = 9001L;

    /** 普通测试商品（库存 10） */
    protected static final Long NORMAL_PRODUCT_ID = 9002L;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    public void resetTestData()
    {
        SecurityContextHolder.clearContext();
        jdbc.update("delete from ct_order_item");
        jdbc.update("delete from ct_order");
        jdbc.update("delete from ct_refund");
        jdbc.update("delete from ct_payment");
        jdbc.update("delete from ct_cart_item");
        jdbc.update("delete from ct_idempotency");
        jdbc.update("update ct_product set stock = 1, status = '1' where product_id = ?", LIMIT_PRODUCT_ID);
        jdbc.update("update ct_product set stock = 10, status = '1' where product_id between 9002 and 9006");
    }

    /**
     * 在当前线程注入登录态。SecurityContextHolder 基于 ThreadLocal，
     * 因此每个并发线程需要各自调用一次。
     */
    protected void loginAs(Long userId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setDeptId(100L);
        user.setUserName("test_" + userId);
        user.setNickName("测试用户" + userId);
        LoginUser loginUser = new LoginUser(userId, 100L, user, new HashSet<>());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList()));
    }

    protected void logout()
    {
        SecurityContextHolder.clearContext();
    }

    protected int stockOf(Long productId)
    {
        return jdbc.queryForObject("select stock from ct_product where product_id = ?", Integer.class, productId);
    }

    protected String statusOf(Long productId)
    {
        return jdbc.queryForObject("select status from ct_product where product_id = ?", String.class, productId);
    }

    protected long countOrders()
    {
        Long n = jdbc.queryForObject("select count(*) from ct_order", Long.class);
        return n == null ? 0L : n;
    }

    protected long countOrdersByGroup(String groupNo)
    {
        Long n = jdbc.queryForObject("select count(*) from ct_order where group_no = ?", Long.class, groupNo);
        return n == null ? 0L : n;
    }

    /** 直接写入购物车条目，返回条目ID（便于并发前置准备） */
    protected Long insertCartItem(Long userId, Long productId, int qty)
    {
        jdbc.update("insert into ct_cart_item(user_id, product_id, quantity, selected, create_time) values (?,?,?,'1',sysdate())",
                userId, productId, qty);
        return jdbc.queryForObject(
                "select cart_item_id from ct_cart_item where user_id = ? and product_id = ?", Long.class, userId, productId);
    }
}
