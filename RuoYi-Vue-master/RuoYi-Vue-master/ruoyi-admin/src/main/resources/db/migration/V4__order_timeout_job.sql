-- 待付款订单超时自动取消：注册定时任务
-- 说明：
--   1) 幂等：按 invoke_target 判断，已存在则不重复插入（Flyway 只执行一次，但多环境手工导入时也需要安全）；
--   2) cron 每分钟执行一次：扫描是走 status+create_time 条件的轻量查询，代价远小于库存被长期占用的业务损失；
--   3) concurrent = '1'（禁止并发）：同一实例内不重叠执行；跨实例的重复执行由"条件更新 + affected 判定"兜底；
--   4) misfire_policy = '3'（放弃本次）：错过的那一轮无需补跑，下一分钟会自然覆盖。
insert into sys_job
    (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status, create_by, create_time, remark)
select '订单超时自动取消', 'SYSTEM', 'campusOrderTask.cancelTimeoutOrders()', '0 0/1 * * * ?', '3', '1', '0', 'system', now(),
       '扫描待付款超过阈值的订单，自动取消并回滚库存；阈值见 campus.order.timeout-minutes'
where not exists (
    select 1 from sys_job where invoke_target = 'campusOrderTask.cancelTimeoutOrders()'
);
