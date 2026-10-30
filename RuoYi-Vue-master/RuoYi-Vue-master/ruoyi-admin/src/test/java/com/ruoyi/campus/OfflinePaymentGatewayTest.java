package com.ruoyi.campus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.ruoyi.campus.payment.OfflinePaymentGateway;

/**
 * 线下面交网关行为：可主动确认、回调一律拒绝。
 *
 * @author campus
 */
class OfflinePaymentGatewayTest
{
    private final OfflinePaymentGateway gateway = new OfflinePaymentGateway();

    @Test
    void offlineGatewaySupportsManualConfirmOnly()
    {
        assertTrue(gateway.supportsManualConfirm());
        assertFalse(gateway.verifyCallback("PAY123", "any"));
        assertNotNull(gateway.confirm("PAY123", new BigDecimal("19.90")));
        assertTrue(gateway.prepay("PAY123", new BigDecimal("19.90")).startsWith("OFFLINE"));
    }
}
