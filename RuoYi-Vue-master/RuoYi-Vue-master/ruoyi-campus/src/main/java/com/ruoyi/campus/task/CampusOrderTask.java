package com.ruoyi.campus.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.campus.service.ICtOrderService;

/**
 * 校园业务定时任务。
 *
 * <p>由若依 Quartz 调度（sys_job 中 invokeTarget = {@code campusOrderTask.cancelTimeoutOrders()}），
 * 所以本类必须是 Spring Bean，且 bean 名与 invokeTarget 前缀一致。
 *
 * <p>为什么不直接用 {@code @Scheduled}：项目已经跑在 Quartz 集群模式（jdbc job store），
 * 任务在后台"系统监控 → 定时任务"里可见、可暂停、可改 cron、有执行日志。
 * 再引入一套 Spring 调度只会多一个不受管的任务。
 *
 * @author campus
 */
@Component("campusOrderTask")
public class CampusOrderTask
{
    private static final Logger log = LoggerFactory.getLogger(CampusOrderTask.class);

    @Autowired
    private ICtOrderService orderService;

    /** 待付款订单超时分钟数；小于等于 0 视为关闭 */
    @Value("${campus.order.timeout-minutes:30}")
    private int timeoutMinutes;

    /** 单次最多处理条数，避免一次扫描过重 */
    @Value("${campus.order.timeout-batch-limit:200}")
    private int batchLimit;

    /**
     * 取消超过阈值仍未支付的订单并回滚库存。
     */
    public void cancelTimeoutOrders()
    {
        if (timeoutMinutes <= 0)
        {
            return;
        }
        try
        {
            int cancelled = orderService.cancelTimeoutOrders(timeoutMinutes, batchLimit);
            if (cancelled > 0)
            {
                log.info("[定时任务] 超时未支付订单已取消 {} 笔", cancelled);
            }
        }
        catch (Exception e)
        {
            // 抛给 Quartz 会整条 job 记失败并触发告警；这里记录后吞掉，
            // 下一分钟会自然重试，避免一次数据库抖动导致任务被标记为 error 状态。
            log.error("[定时任务] 取消超时未支付订单失败，下一轮重试", e);
        }
    }
}
