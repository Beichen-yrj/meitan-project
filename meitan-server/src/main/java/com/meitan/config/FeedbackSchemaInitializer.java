package com.meitan.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedbackSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        addColumnIfMissing("feedback", "status",
                "ALTER TABLE feedback ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/RESOLVED'");
        addColumnIfMissing("feedback", "admin_reply",
                "ALTER TABLE feedback ADD COLUMN admin_reply TEXT DEFAULT NULL COMMENT '管理员回复'");
        addColumnIfMissing("feedback", "handled_by",
                "ALTER TABLE feedback ADD COLUMN handled_by BIGINT DEFAULT NULL COMMENT '处理管理员ID'");
        addColumnIfMissing("feedback", "handled_time",
                "ALTER TABLE feedback ADD COLUMN handled_time DATETIME DEFAULT NULL COMMENT '处理时间'");
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
