package com.ruoyi.campus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 健康状态指标必须真的注册到 Micrometer。
 *
 * <p>这条用例存在的意义：告警规则引用的指标一旦不存在，规则会永远不触发，
 * 而且不会有任何报错。用测试把"指标存在性"钉住，比在规则文件里写注释可靠。
 *
 * @author campus
 */
class CampusHealthMetricsTest extends CampusTestSupport
{
    @Autowired
    private MeterRegistry registry;

    @Test
    @DisplayName("campus_health_status 已注册且健康时为 1")
    void healthGaugeIsRegistered()
    {
        Gauge gauge = registry.find("campus_health_status").gauge();
        assertNotNull(gauge, "campus_health_status 未注册，告警规则将永远不触发");
        assertEquals(1.0, gauge.value(), 0.0001, "应用正常时该指标应为 1");
    }
}