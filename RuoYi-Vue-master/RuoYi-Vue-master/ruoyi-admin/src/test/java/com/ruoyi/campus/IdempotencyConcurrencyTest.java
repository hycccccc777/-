package com.ruoyi.campus;

import java.util.ArrayList;
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
 * 下单幂等测试。
 *
 * 修复前幂等只靠「按 group_no 查一次」，两个并发请求会同时查不到而各下一单。
 * 修复后由 ct_idempotency 的唯一键 uk_ct_idem_key 保证只有一个请求能进入下单流程。
 */
@DisplayName("下单幂等")
class IdempotencyConcurrencyTest extends CampusTestSupport
{
    @Autowired
    private ICtOrderService orderService;

    @Test
    @DisplayName("同一幂等键并发提交 10 次，只产生 1 笔订单且库存只扣 1 次")
    void 同一幂等键并发提交只产生一笔订单() throws Exception
    {
        Long buyer = 2001L;
        Long cartA = insertCartItem(buyer, 9002L, 1);
        Long cartB = insertCartItem(buyer, 9003L, 1);
        String key = "idem-" + System.nanoTime();

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                loginAs(buyer);
                ready.countDown();
                go.await();
                try {
                    orderService.createOrder(buildBody(cartA, cartB, key));
                    return Boolean.TRUE;
                }
                catch (Exception e) {
                    return Boolean.FALSE;
                }
                finally {
                    logout();
                }
            }));
        }

        assertTrue(ready.await(20, TimeUnit.SECONDS), "并发线程未就绪");
        go.countDown();

        int success = 0;
        for (Future<Boolean> f : futures) {
            if (Boolean.TRUE.equals(f.get(30, TimeUnit.SECONDS))) {
                success++;
            }
        }
        pool.shutdownNow();

        assertTrue(success >= 1, "至少应有一个请求成功");
        assertEquals(1L, countOrders(), "并发提交同一幂等键只能产生 1 笔订单，实际 " + countOrders());
        assertEquals(1L, countOrdersByGroup(key), "该幂等键下只能有 1 笔订单");
        assertEquals(9, stockOf(9002L), "商品 9002 的库存只能被扣减 1 次");
        assertEquals(9, stockOf(9003L), "商品 9003 的库存只能被扣减 1 次");
    }

    @Test
    @DisplayName("顺序重复提交同一幂等键，返回原批次且不新增订单")
    void 顺序重复提交返回原批次()
    {
        Long buyer = 2002L;
        Long cartA = insertCartItem(buyer, 9002L, 1);
        String key = "idem-seq-" + System.nanoTime();

        loginAs(buyer);
        try {
            Map<String, Object> first = orderService.createOrder(buildBody(cartA, null, key));
            String groupNo = (String) first.get("groupNo");

            // 第二次：即使购物车已被清空，也应直接重放原批次而不报错
            Map<String, Object> second = orderService.createOrder(buildBody(cartA, null, key));

            assertEquals(groupNo, second.get("groupNo"), "应返回与首次相同的批次号");
            assertEquals(1L, countOrders(), "重复提交不得新增订单");
            assertEquals(9, stockOf(9002L), "重复提交不得重复扣减库存");
        }
        finally {
            logout();
        }
    }

    private CtOrderCreateBody buildBody(Long cartA, Long cartB, String key)
    {
        List<Long> ids = new ArrayList<>();
        ids.add(cartA);
        if (cartB != null) {
            ids.add(cartB);
        }
        CtOrderCreateBody body = new CtOrderCreateBody();
        body.setCartItemIds(ids);
        body.setReceiver("幂等测试");
        body.setPhone("13800000000");
        body.setAddressDetail("幂等测试地址");
        body.setIdempotencyKey(key);
        return body;
    }
}
