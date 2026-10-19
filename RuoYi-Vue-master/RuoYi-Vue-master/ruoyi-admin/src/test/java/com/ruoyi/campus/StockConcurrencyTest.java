package com.ruoyi.campus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.campus.domain.CtOrderCreateBody;
import com.ruoyi.campus.service.ICtOrderService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 并发下单的库存正确性测试。
 *
 * 修复前扣库存是「先查 stock，再减，再写回」，20 个线程会同时读到 stock=1 并全部扣减成功 -> 超卖。
 * 修复后改为单条条件 UPDATE，由 InnoDB 行锁保证只有一个请求成功。
 */
@DisplayName("库存并发安全")
class StockConcurrencyTest extends CampusTestSupport
{
    @Autowired
    private ICtOrderService orderService;

    @Test
    @DisplayName("库存为 1 时，20 个买家并发下单只能成功 1 单")
    void 库存为1时并发下单只能成功一单() throws Exception
    {
        List<String> failures1 = Collections.synchronizedList(new ArrayList<>());
        Map<Long, Long> cartIds = new HashMap<>();
        for (Long buyer : BUYER_IDS) {
            cartIds.put(buyer, insertCartItem(buyer, LIMIT_PRODUCT_ID, 1));
        }

        int success = 并发下单(cartIds, failures1).size();

        assertEquals(1, success, "库存为 1 时只允许 1 个请求下单成功，实际成功 " + success + " 单；失败原因=" + failures1);
        assertEquals(1L, countOrders(), "数据库中只能存在 1 笔订单");
        assertEquals(0, stockOf(LIMIT_PRODUCT_ID), "库存应被扣减为 0");
        assertEquals("4", statusOf(LIMIT_PRODUCT_ID), "售罄商品应自动置为「已售出」");
    }

    @Test
    @DisplayName("库存为 10 时，20 个买家并发下单恰好成功 10 单（无丢失更新）")
    void 库存为10时并发下单恰好成功十单() throws Exception
    {
        List<String> failures2 = Collections.synchronizedList(new ArrayList<>());
        Map<Long, Long> cartIds = new HashMap<>();
        for (Long buyer : BUYER_IDS) {
            cartIds.put(buyer, insertCartItem(buyer, NORMAL_PRODUCT_ID, 1));
        }

        List<Long> winners = 并发下单(cartIds, failures2);
        int success = winners.size();
        int stockNow = stockOf(NORMAL_PRODUCT_ID);
        long ordersNow = countOrders();

        assertEquals(10, success, "库存为 10 时应恰好成功 10 单 | success=" + success
                + " stock=" + stockNow + " orders=" + ordersNow
                + " winners=" + winners + " failures=" + failures2);
        assertEquals(10L, ordersNow, "订单数应与成功数一致");
        assertEquals(0, stockNow, "库存应恰好扣减为 0");
    }

    /** 让所有线程在同一起点同时下单，返回成功线程数，并把失败原因收集到 failures */
    private List<Long> 并发下单(Map<Long, Long> cartIds, List<String> failures) throws Exception
    {
        int n = cartIds.size();
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch ready = new CountDownLatch(n);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();

        for (Map.Entry<Long, Long> entry : cartIds.entrySet()) {
            Long buyer = entry.getKey();
            Long cartItemId = entry.getValue();
            futures.add(pool.submit(() -> {
                loginAs(buyer);
                ready.countDown();
                go.await();
                try {
                    CtOrderCreateBody body = new CtOrderCreateBody();
                    body.setCartItemIds(List.of(cartItemId));
                    body.setReceiver("并发测试");
                    body.setPhone("13800000000");
                    body.setAddressDetail("并发测试地址");
                    orderService.createOrder(body);
                    return buyer;
                }
                catch (Exception e) {
                    failures.add(buyer + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    return null;
                }
                finally {
                    logout();
                }
            }));
        }

        assertTrue(ready.await(20, TimeUnit.SECONDS), "并发线程未在预期时间内就绪");
        go.countDown();

        List<Long> winners = new ArrayList<>();
        for (Future<Long> f : futures) {
            Long b = f.get(30, TimeUnit.SECONDS);
            if (b != null) {
                winners.add(b);
            }
        }
        pool.shutdownNow();
        return winners;
    }
}
