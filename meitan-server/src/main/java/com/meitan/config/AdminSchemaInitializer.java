package com.meitan.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 对已有数据库进行无损升级，避免用户必须重新执行会清空数据的 init.sql。
 */
@Component
@RequiredArgsConstructor
public class AdminSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("sys_user", "is_blacklisted",
                "ALTER TABLE sys_user ADD COLUMN is_blacklisted TINYINT NOT NULL DEFAULT 0 COMMENT '是否在黑名单：1-是 0-否'");
        addColumnIfMissing("sys_user", "blacklist_reason",
                "ALTER TABLE sys_user ADD COLUMN blacklist_reason VARCHAR(500) DEFAULT NULL COMMENT '加入黑名单原因'");
        addColumnIfMissing("sys_user", "last_login_time",
                "ALTER TABLE sys_user ADD COLUMN last_login_time DATETIME DEFAULT NULL COMMENT '最近登录时间'");
        addColumnIfMissing("sys_user", "last_login_ip",
                "ALTER TABLE sys_user ADD COLUMN last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最近登录IP'");
        addColumnIfMissing("sys_user", "login_count",
                "ALTER TABLE sys_user ADD COLUMN login_count INT NOT NULL DEFAULT 0 COMMENT '成功登录次数'");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS sys_login_log (
                id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                user_id BIGINT DEFAULT NULL COMMENT '用户ID，未知账号登录时为空',
                username VARCHAR(50) NOT NULL COMMENT '登录用户名',
                login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
                login_ip VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
                user_agent VARCHAR(500) DEFAULT NULL COMMENT '浏览器信息',
                success TINYINT NOT NULL DEFAULT 0 COMMENT '结果：1-成功 0-失败',
                failure_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
                PRIMARY KEY (id),
                KEY idx_login_user_id (user_id),
                KEY idx_login_time (login_time),
                KEY idx_login_success (success)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志表'
            """);
    }

    private void addColumnIfMissing(String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }
}
