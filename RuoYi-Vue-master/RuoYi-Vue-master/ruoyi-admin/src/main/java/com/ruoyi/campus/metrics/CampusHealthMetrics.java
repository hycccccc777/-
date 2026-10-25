package com.ruoyi.campus.metrics;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 把健康检查结果暴露成指标，供告警规则使用。
 *
 * <p>为什么需要：Actuator 的 /actuator/health 是端点，不是指标。
 * 告警规则里写 {@code health{...}} 会得到空结果集，规则永远不会触发 ——
 * 页面看着正常，DB 挂了也没人知道。这里补一个 gauge，规则才有东西可算。
 *
 * <p>取值：1 = UP，0 = DOWN / OUT_OF_SERVICE / 查询失败。
 *
 * @author campus
 */
@Component
public class CampusHealthMetrics
{
    private final ObjectProvider<HealthEndpoint> healthEndpoint;

    public CampusHealthMetrics(MeterRegistry registry, ObjectProvider<HealthEndpoint> healthEndpoint)
    {
        this.healthEndpoint = healthEndpoint;
        Gauge.builder("campus_health_status", this, CampusHealthMetrics::current)
                .description("应用健康状态：1=UP，0=非 UP")
                .register(registry);
    }

    private double current()
    {
        HealthEndpoint endpoint = healthEndpoint.getIfAvailable();
        if (endpoint == null)
        {
            return 0;
        }
        try
        {
            HealthDescriptor descriptor = endpoint.health();
            return Status.UP.equals(descriptor.getStatus()) ? 1 : 0;
        }
        catch (RuntimeException e)
        {
            // 健康检查自身抛错时按不健康处理，避免口径漂移
            return 0;
        }
    }
}