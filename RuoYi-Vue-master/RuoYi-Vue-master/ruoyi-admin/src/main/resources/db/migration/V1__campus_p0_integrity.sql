-- =====================================================================
-- V1 校园交易核心正确性加固（P0）
-- 说明：基线库由 sql/ 目录的历史脚本导入，本文件只记录「后续增量」，
--       导入顺序为 ry -> quartz -> campus_* -> 本文件。
-- =====================================================================

-- 1) 下单幂等表：让「同一幂等键只产生一批订单」由数据库唯一键保证，
--    而不是靠应用层先查后写（并发下会重复下单）。
create table if not exists ct_idempotency (
  idem_id     bigint(20)   not null auto_increment comment 'id',
  idem_key    varchar(128) not null comment 'idempotent key from client',
  group_no    varchar(64)  default null comment 'order group no, filled when finished',
  status      char(1)      default '0' comment '0 processing, 1 finished',
  create_time datetime     comment 'create time',
  primary key (idem_id),
  unique key uk_ct_idem_key (idem_key)
) engine=innodb comment='order idempotency table';

-- 2) group_no 需要容纳 "G" + 32 位 UUID，varchar(32) 会导致插入被截断。
alter table ct_order modify column group_no varchar(64) default null comment 'order group no / idempotency key';

-- 3) 同一次结算内同一卖家只能有一条子订单，防止并发下重复建单。
alter table ct_order add unique key uk_ct_order_group_seller (group_no, seller_id);