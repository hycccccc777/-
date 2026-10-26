package com.ruoyi.campus;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.campus.domain.CtOrderCreateBody;
import com.ruoyi.campus.service.ICtOrderService;
import com.ruoyi.campus.service.ICtPaymentService;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 待付款订单超时自动取消。
 *
 * <p>这条规则要防的是：下单不付款把库存长期占住，恶意刷单能把商品直接锁死。
 * 测试重点不是"能不能取消"，而是边界——没到点的不动、已付款的不动、
 * 并发/多实例下不会重复回滚库存。
 */
@DisplayName("订单超时自动取消")
class OrderTimeoutTest extends CampusTestSupport
{
    @Autowired
    private ICtOrderService orderService;

    @Autowired
    private ICtPaymentService paymentService;

    @Test
    @DisplayName("超过阈值仍未支付：取消并回滚库存")
    void 超时订单被取消并回滚库存()
    {
        Long buyer = 2011L;
        loginAs(buyer);
        try
        {
            Long cartId = insertCartItem(buyer, NORMAL_PRODUCT_ID, 1);
            String orderNo = createOrder(cartId);
            assertEquals("0", orderStatus(orderNo), "下单后应为待付款");
            assertEquals(9, stockOf(NORMAL_PRODUCT_ID), "下单应扣减库存");

            backdate(orderNo, 31);

            int cancelled = orderService.cancelTimeoutOrders(30, 200);

            assertEquals(1, cancelled, "应取消 1 笔超时订单");
            assertEquals("4", orderStatus(orderNo), "订单应变为已取消");
            assertEquals("超时未支付", cancelReason(orderNo), "应记录取消原因");
            assertEquals(10, stockOf(NORMAL_PRODUCT_ID), "取消后库存必须回滚");
        }
        finally
        {
            logout();
        }
    }

    @Test
    @DisplayName("未到阈值：不动，库存仍被占用")
    void 未超时订单不被取消()
    {
        Long buyer = 2012L;
        loginAs(buyer);
        try
        {
            Long cartId = insertCartItem(buyer, NORMAL_PRODUCT_ID, 1);
            String orderNo = createOrder(cartId);

            int cancelled = orderService.cancelTimeoutOrders(30, 200);

            assertEquals(0, cancelled, "未到阈值不应取消");
            assertEquals("0", orderStatus(orderNo), "订单仍应为待付款");
            assertEquals(9, stockOf(NORMAL_PRODUCT_ID), "库存应保持被占用");
        }
        finally
        {
            logout();
        }
    }

    @Test
    @DisplayName("已支付订单即使时间超了也不会被误取消")
    void 已支付订单不受影响()
    {
        Long buyer = 2013L;
        loginAs(buyer);
        try
        {
            Long cartId = insertCartItem(buyer, NORMAL_PRODUCT_ID, 1);
            String orderNo = createOrder(cartId);

            Map<String, Object> pay = paymentService.create(buyer, orderNo, "0");
            paymentService.confirm(buyer, (String) pay.get("payNo"));
            assertEquals("1", orderStatus(orderNo), "支付后应为待发货");

            backdate(orderNo, 31);
            int cancelled = orderService.cancelTimeoutOrders(30, 200);

            assertEquals(0, cancelled, "已支付订单不应被取消");
            assertEquals("1", orderStatus(orderNo), "订单仍应为待发货");
            assertEquals(9, stockOf(NORMAL_PRODUCT_ID), "已支付订单库存不应回滚");
        }
        finally
        {
            logout();
        }
    }

    @Test
    @DisplayName("单批上限生效：一次只处理 limit 笔，剩余下一轮继续")
    void 批量上限生效()
    {
        Long buyer = 2014L;
        loginAs(buyer);
        try
        {
            // 购物车对 (user, product) 有唯一键，用三件不同商品各下一单
            String no1 = createOrder(insertCartItem(buyer, 9002L, 1));
            String no2 = createOrder(insertCartItem(buyer, 9003L, 1));
            String no3 = createOrder(insertCartItem(buyer, 9004L, 1));
            backdate(no1, 31);
            backdate(no2, 31);
            backdate(no3, 31);

            int first = orderService.cancelTimeoutOrders(30, 2);
            assertEquals(2, first, "单批上限 2 应只取消 2 笔");

            int pending = jdbc.queryForObject(
                    "select count(*) from ct_order where status = '0'", Integer.class);
            assertEquals(1, pending, "应剩 1 笔待付款留到下一轮");

            int second = orderService.cancelTimeoutOrders(30, 2);
            assertEquals(1, second, "下一轮应取消剩余 1 笔");

            Integer totalStock = jdbc.queryForObject(
                    "select sum(stock) from ct_product where product_id in (9002,9003,9004)", Integer.class);
            assertEquals(30, totalStock, "全部取消后三件商品库存都应回到 10");
        }
        finally
        {
            logout();
        }
    }

    @Test
    @DisplayName("重复执行不会二次回滚库存（幂等）")
    void 重复执行幂等()
    {
        Long buyer = 2015L;
        loginAs(buyer);
        try
        {
            String orderNo = createOrder(insertCartItem(buyer, NORMAL_PRODUCT_ID, 1));
            backdate(orderNo, 31);

            assertEquals(1, orderService.cancelTimeoutOrders(30, 200));
            assertEquals(0, orderService.cancelTimeoutOrders(30, 200), "已取消的订单不应再次命中");
            assertEquals(10, stockOf(NORMAL_PRODUCT_ID), "库存只回滚一次");
        }
        finally
        {
            logout();
        }
    }

    private String createOrder(Long cartItemId)
    {
        CtOrderCreateBody body = new CtOrderCreateBody();
        body.setCartItemIds(List.of(cartItemId));
        body.setReceiver("超时测试");
        body.setPhone("13800000000");
        body.setAddressDetail("超时测试地址");
        Map<String, Object> result = orderService.createOrder(body);
        List<?> orders = (List<?>) result.get("orders");
        assertEquals(1, orders.size(), "同一卖家应只生成 1 笔订单");
        return (String) ((Map<?, ?>) orders.get(0)).get("orderNo");
    }

    private void backdate(String orderNo, int minutes)
    {
        jdbc.update("update ct_order set create_time = date_sub(now(), interval ? minute) where order_no = ?",
                minutes, orderNo);
    }

    private String orderStatus(String orderNo)
    {
        return jdbc.queryForObject("select status from ct_order where order_no = ?", String.class, orderNo);
    }

    private String cancelReason(String orderNo)
    {
        return jdbc.queryForObject("select cancel_reason from ct_order where order_no = ?", String.class, orderNo);
    }
}
