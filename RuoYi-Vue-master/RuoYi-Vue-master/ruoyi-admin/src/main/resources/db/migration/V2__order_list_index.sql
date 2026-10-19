-- =====================================================================
-- V2 订单查询性能索引
-- 「我买到的 / 我卖出的订单」都按状态 + 用户维度查询，
-- 原先只有 buyer_id 单列索引，订单量上来后需要回表过滤 status。
-- =====================================================================
alter table ct_order add key idx_ct_order_buyer_status (buyer_id, status);
alter table ct_order add key idx_ct_order_seller_status (seller_id, status);