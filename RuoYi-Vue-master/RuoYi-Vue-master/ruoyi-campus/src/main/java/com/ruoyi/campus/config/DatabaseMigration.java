package com.ruoyi.campus.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 数据库版本迁移。
 *
 * <p>为什么不用 Spring Boot 自动配置：Spring Boot 4 把各自动配置拆成独立模块，
 * flyway-core 在 classPath 上并不一定会触发 FlywayAutoConfiguration。这里显式调用，
 * 行为可控，也避免出现「yml 配了却没生效」的黑洞。
 *
 * <p>配置集中在本类，不再散落在 application.yml，确保单一事实来源。
 *
 * @author campus
 */
@Component
public class DatabaseMigration implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(DatabaseMigration.class);

    private final DataSource dataSource;

    public DatabaseMigration(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args)
    {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                // 存量库由 sql/*.sql 导入，已包含 V1 的变更，
                // 因此基线版本设为 1：只登记基线，不重复执行 V1
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .baselineDescription("legacy schema imported from sql/*.sql")
                .load();

        var result = flyway.migrate();
        if (result.migrationsExecuted == 0)
        {
            log.info("数据库无需迁移，当前版本 {}", result.targetSchemaVersion);
        }
        else
        {
            log.info("已执行 {} 个数据库迁移，目标版本 {}", result.migrationsExecuted, result.targetSchemaVersion);
        }
    }
}