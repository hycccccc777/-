-- 上线清理：移除演示卖家(1001-1004)与其演示商品，保留真实数据。
-- 幂等：可重复执行；没有演示数据时什么也不做。
DELETE FROM ct_cart_item     WHERE product_id IN (SELECT product_id FROM ct_product WHERE seller_id IN (1001,1002,1003,1004));
DELETE FROM ct_favorite      WHERE product_id IN (SELECT product_id FROM ct_product WHERE seller_id IN (1001,1002,1003,1004));
DELETE FROM ct_product_image WHERE product_id IN (SELECT product_id FROM ct_product WHERE seller_id IN (1001,1002,1003,1004));
DELETE FROM ct_product       WHERE seller_id IN (1001,1002,1003,1004);
DELETE FROM sys_user_role    WHERE user_id IN (1001,1002,1003,1004);
DELETE FROM sys_user_post    WHERE user_id IN (1001,1002,1003,1004);
DELETE FROM sys_user         WHERE user_id IN (1001,1002,1003,1004);
