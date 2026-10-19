package com.ruoyi.campus;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.domain.CtOrderCreateBody;
import com.ruoyi.campus.service.ICtOrderService;
import com.ruoyi.campus.service.ICtPaymentService;
import com.ruoyi.campus.service.ICtRefundService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 订单 / 支付 / 退款全链路回归测试，覆盖状态机约束与越权防护。
 */
@DisplayName("订单全链路回归")
class OrderLifecycleTest extends CampusTestSupport
{
    @Autowired
    private ICtOrderService orderService;

    @Autowired
    private ICtPaymentService paymentService;

    @Autowired
    private ICtRefundService refundService;

    @Test
    @DisplayName("下单 → 支付 → 发货 → 确认收货，状态与库存均正确")
    void 完整交易链路()
    {
        Long buyer = 2003L;
        Long cartId = insertCartItem(buyer, NORMAL_PRODUCT_ID, 1);

        loginAs(buyer);
        try {
            String orderNo = createOrder(buyer, cartId);
            assertEquals("0", statusOfOrder(buyer, orderNo), "下单后应为待付款");
            assertEquals(9, stockOf(NORMAL_PRODUCT_ID), "下单应扣减库存");

            Map<String, Object> pay = paymentService.create(buyer, orderNo, "0");
            String payNo = (String) pay.get("payNo");
            assertEquals("0", pay.get("status"), "新建支付单应为待支付");

            paymentService.confirm(buyer, payNo);
            assertEquals("1", statusOfOrder(buyer, orderNo), "支付成功后应为待发货");

            paymentService.confirm(buyer, payNo);
            assertEquals("1", paymentService.getStatus(buyer, payNo).get("status"), "重复确认支付应幂等");

            loginAs(SELLER_ID);
            orderService.shipOrder(SELLER_ID, orderNo);
            assertEquals("2", statusOfOrder(buyer, orderNo), "发货后应为待收货");

            loginAs(buyer);
            orderService.confirmOrder(buyer, orderNo);
            assertEquals("3", statusOfOrder(buyer, orderNo), "确认收货后应为已完成");
        }
        finally {
            logout();
        }
    }

    @Test
    @DisplayName("取消待付款订单应回滚库存，商品重新上架")
    void 取消订单回滚库存()
    {
        Long buyer = 2004L;
        Long cartId = insertCartItem(buyer, NORMAL_PRODUCT_ID, 1);

        loginAs(buyer);
        try {
            String orderNo = createOrder(buyer, cartId);
            assertEquals(9, stockOf(NORMAL_PRODUCT_ID));

            orderService.cancelOrder(buyer, orderNo, "不想要了");
            assertEquals("4", statusOfOrder(buyer, orderNo), "取消后应为已取消");
            assertEquals(10, stockOf(NORMAL_PRODUCT_ID), "取消订单必须回滚库存");
            assertEquals("1", statusOf(NORMAL_PRODUCT_ID), "商品应恢复为已上架");
        }
        finally {
            logout();
        }
    }

    @Test
    @DisplayName("退款被卖家拒绝时，订单回滚到申请前的状态")
    void 退款拒绝后订单回滚()
    {
        Long buyer = 2005L;
        Long cartId = insertCartItem(buyer, NORMAL_PRODUCT_ID, 1);

        loginAs(buyer);
        try {
            String orderNo = createOrder(buyer, cartId);
            Map<String, Object> refund = refundService.apply(buyer, orderNo, "买错了");
            assertEquals("0", refund.get("status"), "申请后应为待审核");
            assertEquals("5", statusOfOrder(buyer, orderNo), "申请后订单应为退款中");

            loginAs(SELLER_ID);
            refundService.reject(SELLER_ID, (String) refund.get("refundNo"), "商品没问题");
            assertEquals("2", refundService.listIncoming(SELLER_ID).get(0).get("status"), "退款单应为已拒绝");

            loginAs(buyer);
            assertEquals("0", statusOfOrder(buyer, orderNo), "拒绝后订单应回滚为申请前的待付款");
            assertEquals(9, stockOf(NORMAL_PRODUCT_ID), "拒绝退款不应回滚库存（订单未支付成功场景外的原始扣减保持）");
        }
        finally {
            logout();
        }
    }

    @Test
    @DisplayName("退款同意并完成后，订单为已退款且库存回滚")
    void 退款完成后库存回滚()
    {
        Long buyer = 2006L;
        Long cartId = insertCartItem(buyer, LIMIT_PRODUCT_ID, 1);

        loginAs(buyer);
        try {
            String orderNo = createOrder(buyer, cartId);
            assertEquals(0, stockOf(LIMIT_PRODUCT_ID));
            assertEquals("4", statusOf(LIMIT_PRODUCT_ID), "售罄商品应为已售出");

            Map<String, Object> refund = refundService.apply(buyer, orderNo, "不想要了");
            String refundNo = (String) refund.get("refundNo");

            loginAs(SELLER_ID);
            refundService.agree(SELLER_ID, refundNo);
            refundService.complete(SELLER_ID, refundNo);

            assertEquals("3", refundService.listIncoming(SELLER_ID).get(0).get("status"), "退款单应为已退款完成");
            assertEquals("6", statusOfOrder(buyer, orderNo), "订单应为已退款");
            assertEquals(1, stockOf(LIMIT_PRODUCT_ID), "退款完成后必须回滚库存");
            assertEquals("1", statusOf(LIMIT_PRODUCT_ID), "商品应重新上架");
        }
        finally {
            logout();
        }
    }

    @Test
    @DisplayName("越权防护：买家不能发货，他人订单不可见")
    void 越权防护()
    {
        Long buyer = 2007L;
        Long other = 2008L;
        Long cartId = insertCartItem(buyer, NORMAL_PRODUCT_ID, 1);

        loginAs(buyer);
        try {
            String orderNo = createOrder(buyer, cartId);
            Map<String, Object> pay = paymentService.create(buyer, orderNo, "0");
            paymentService.confirm(buyer, (String) pay.get("payNo"));

            // 买家不是卖家，不能发货
            assertThrows(ApiException.class, () -> orderService.shipOrder(buyer, orderNo));

            // 第三方不能查看该订单
            assertThrows(ApiException.class, () -> orderService.orderDetail(other, orderNo));
        }
        finally {
            logout();
        }
    }

    private String createOrder(Long buyer, Long cartItemId)
    {
        CtOrderCreateBody body = new CtOrderCreateBody();
        body.setCartItemIds(List.of(cartItemId));
        body.setReceiver("回归测试");
        body.setPhone("13800000000");
        body.setAddressDetail("回归测试地址");
        Map<String, Object> result = orderService.createOrder(body);
        List<?> orders = (List<?>) result.get("orders");
        assertEquals(1, orders.size(), "同一卖家的商品应只生成 1 笔订单");
        return (String) ((Map<?, ?>) orders.get(0)).get("orderNo");
    }

    private String statusOfOrder(Long userId, String orderNo)
    {
        return (String) orderService.orderDetail(userId, orderNo).get("status");
    }
}
