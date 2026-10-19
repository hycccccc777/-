package com.ruoyi.campus;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.campus.api.ApiException;
import com.ruoyi.campus.domain.CtOrderCreateBody;
import com.ruoyi.campus.payment.MockPaymentGateway;
import com.ruoyi.campus.payment.PaymentCallbackSecret;
import com.ruoyi.campus.payment.PaymentGateway;
import com.ruoyi.campus.payment.PaymentGatewayRegistry;
import com.ruoyi.campus.service.ICtOrderService;
import com.ruoyi.campus.service.ICtPaymentService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付网关抽象测试：验证「换渠道不改业务代码」成立，
 * 且沙箱之外的分支（失败、不支持主动确认、回调签名）也有覆盖。
 */
@DisplayName("支付网关抽象")
class PaymentGatewayTest extends CampusTestSupport
{
    @Autowired
    private ICtOrderService orderService;

    @Autowired
    private ICtPaymentService paymentService;

    @Autowired
    private PaymentGatewayRegistry registry;

    @Autowired
    private PaymentCallbackSecret secret;

    @Test
    @DisplayName("注册表按编码分发，未知编码降级到沙箱")
    void 注册表按编码分发()
    {
        assertEquals("沙箱支付", registry.resolve("0").name());
        assertEquals("支付模拟网关", registry.resolve(MockPaymentGateway.CODE).name());
        // 未注册的支付方式不应让演示环境直接不可用，而是降级到沙箱
        assertEquals("沙箱支付", registry.resolve("999").name());
        // require 则必须精确匹配，用于回调等场景
        assertThrows(ApiException.class, () -> registry.require("999"));
    }

    @Test
    @DisplayName("创建支付单返回网关名称与预下单凭据")
    void 创建支付单带预下单凭据()
    {
        Long buyer = 2011L;
        Long cartId = insertCartItem(buyer, 9002L, 1);
        loginAs(buyer);
        try {
            String orderNo = createOrder(buyer, cartId);
            Map<String, Object> pay = paymentService.create(buyer, orderNo, "0");
            assertEquals("沙箱支付", pay.get("gateway"));
            assertNotNull(pay.get("prepayData"));
            assertTrue(String.valueOf(pay.get("prepayData")).contains("SANDBOX_PREPAY:"));
            assertEquals("0", pay.get("status"));
        }
        finally {
            logout();
        }
    }

    @Test
    @DisplayName("模拟网关：金额超阈值时确认失败，订单保持待付款")
    void 模拟网关失败分支()
    {
        Long buyer = 2012L;
        // 9003 售价 599 元，超过模拟网关的失败阈值
        Long cartId = insertCartItem(buyer, 9003L, 1);
        loginAs(buyer);
        try {
            String orderNo = createOrder(buyer, cartId);
            Map<String, Object> pay = paymentService.create(buyer, orderNo, MockPaymentGateway.CODE);
            String payNo = (String) pay.get("payNo");

            assertThrows(ApiException.class, () -> paymentService.confirm(buyer, payNo));
            // 失败后支付单仍待支付、订单仍待付款，不能出现「钱没到账但订单已发货」
            assertEquals("0", paymentService.getStatus(buyer, payNo).get("status"));
            assertEquals("0", orderService.orderDetail(buyer, orderNo).get("status"));
        }
        finally {
            logout();
        }
    }

    @Test
    @DisplayName("异步回调：签名不对一律拒绝，签名正确才推进订单，重复回调幂等")
    void 回调必须校验签名()
    {
        Long buyer = 2013L;
        Long cartId = insertCartItem(buyer, 9002L, 1);
        loginAs(buyer);
        try {
            String orderNo = createOrder(buyer, cartId);
            Map<String, Object> pay = paymentService.create(buyer, orderNo, "0");
            String payNo = (String) pay.get("payNo");

            // 伪造回调：不带签名或签名错误都不能把订单刷成已付款
            assertThrows(ApiException.class, () -> paymentService.handleCallback(payNo, null));
            assertThrows(ApiException.class, () -> paymentService.handleCallback(payNo, "deadbeef"));
            assertEquals("0", orderService.orderDetail(buyer, orderNo).get("status"));

            paymentService.handleCallback(payNo, secret.sign(payNo));
            assertEquals("1", orderService.orderDetail(buyer, orderNo).get("status"));
            assertEquals("1", paymentService.getStatus(buyer, payNo).get("status"));

            // 渠道重复回调必须幂等
            paymentService.handleCallback(payNo, secret.sign(payNo));
            assertEquals("1", orderService.orderDetail(buyer, orderNo).get("status"));
        }
        finally {
            logout();
        }
    }

    @Test
    @DisplayName("网关实现彼此独立：新增渠道只需加一个实现类")
    void 网关可扩展()
    {
        List<String> codes = registry.supportedCodes();
        assertTrue(codes.contains("0"), "沙箱网关应已注册");
        assertTrue(codes.contains(MockPaymentGateway.CODE), "模拟网关应已注册");
        assertTrue(registry.describe().size() >= 2);
        assertTrue(registry.require("0").supportsManualConfirm());
    }

    private String createOrder(Long buyer, Long cartItemId)
    {
        CtOrderCreateBody body = new CtOrderCreateBody();
        body.setCartItemIds(List.of(cartItemId));
        body.setReceiver("支付测试");
        body.setPhone("13800000000");
        body.setAddressDetail("支付测试地址");
        Map<String, Object> result = orderService.createOrder(body);
        List<?> orders = (List<?>) result.get("orders");
        return (String) ((Map<?, ?>) orders.get(0)).get("orderNo");
    }
}