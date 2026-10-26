package com.ruoyi.campus.bootstrap;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import com.ruoyi.quartz.domain.SysJob;
import com.ruoyi.quartz.mapper.SysJobMapper;
import com.ruoyi.quartz.util.ScheduleUtils;

/**
 * 定时任务补注册。
 *
 * <p>解决一个真实存在的启动顺序问题：若依的 {@code SysJobServiceImpl#init()} 在
 * {@code @PostConstruct} 阶段读取 sys_job 并注册到 Quartz，而 Flyway 迁移
 * （如 V4 新增的订单超时任务）可能在其之后才执行完。结果是「数据库里新增了任务，
 * 但调度器里没有」，要重启一次才会生效——这很容易被误判成"功能没做"。
 *
 * <p>本类在应用完全就绪后（Flyway 已完成）再做一次兜底：只给「数据库里是启用状态、
 * 但调度器里不存在」的任务补建触发器，已存在的跳过。多实例同时启动时，
 * 后启动的实例会因 checkExists=true 而跳过，不会重复建。
 *
 * @author campus
 */
@Component
public class CampusJobBootstrap implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(CampusJobBootstrap.class);

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private SysJobMapper jobMapper;

    @Override
    public void run(ApplicationArguments args)
    {
        try
        {
            for (SysJob job : jobMapper.selectJobAll())
            {
                if (!"0".equals(job.getStatus()))
                {
                    continue;
                }
                JobKey key = ScheduleUtils.getJobKey(job.getJobId(), job.getJobGroup());
                if (!scheduler.checkExists(key))
                {
                    ScheduleUtils.createScheduleJob(scheduler, job);
                    log.info("[job-bootstrap] 补注册定时任务：{}（{}）", job.getInvokeTarget(), job.getCronExpression());
                }
            }
        }
        catch (Exception e)
        {
            // 补注册失败不应阻断启动；下一次重启会再次尝试，运行期管理员也可在后台手动恢复。
            log.warn("[job-bootstrap] 定时任务补注册失败（不影响启动）", e);
        }
    }
}
