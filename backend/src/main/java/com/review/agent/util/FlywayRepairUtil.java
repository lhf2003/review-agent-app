package com.review.agent.util;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Flyway 修复工具类.
 * 用于修复失败的迁移记录.
 */
@Component
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayRepairUtil {

    /**
     * 执行 Flyway repair 命令.
     * 修复失败的迁移记录.
     */
    public static void repair(String url, String username, String password) {
        DataSource dataSource = new DriverManagerDataSource(url, username, password);

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();

        // 执行 repair
        flyway.repair();
        System.out.println("Flyway repair completed successfully!");
    }

    /**
     * 手动删除失败的迁移记录（备用方案）.
     */
    public static void manualRepair(String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 删除失败的迁移记录
        int deleted = jdbcTemplate.update(
            "DELETE FROM flyway_schema_history WHERE success = 0"
        );
        System.out.println("Deleted " + deleted + " failed migration records.");
    }

    /**
     * 强力修复 - 删除版本1的所有记录并重新 baseline.
     */
    public static void forceRepair(String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 删除版本1的所有记录
        int deleted = jdbcTemplate.update(
            "DELETE FROM flyway_schema_history WHERE version = '1'"
        );
        System.out.println("Deleted " + deleted + " records for version 1.");

        // 显示当前状态
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history", Integer.class
        );
        System.out.println("Current migration count: " + count);
    }

    /**
     * 主方法 - 用于命令行执行.
     */
    public static void main(String[] args) {
        // 默认连接信息
        String url = "jdbc:mysql://localhost:3306/review_agent?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "123456";

        // 可以通过参数传入连接信息
        if (args.length >= 1) url = args[0];
        if (args.length >= 2) username = args[1];
        if (args.length >= 3) password = args[2];

        System.out.println("Starting Flyway repair...");
        System.out.println("URL: " + url);
        System.out.println("Username: " + username);

        try {
            repair(url, username, password);
        } catch (Exception e) {
            System.err.println("Flyway repair failed, trying manual repair...");
            e.printStackTrace();
            manualRepair(url, username, password);
        }
    }
}
