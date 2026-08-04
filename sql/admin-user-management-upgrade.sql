-- 管理员用户管理功能的无损数据库升级脚本。
-- 正常启动新版后端时 AdminSchemaInitializer 会自动执行同等升级；
-- 仅在需要手工维护数据库时使用本文件，并根据数据库当前字段情况逐条执行。

USE meitan;

ALTER TABLE sys_user
    ADD COLUMN is_blacklisted TINYINT NOT NULL DEFAULT 0 COMMENT '是否在黑名单：1-是 0-否',
    ADD COLUMN blacklist_reason VARCHAR(500) DEFAULT NULL COMMENT '加入黑名单原因',
    ADD COLUMN last_login_time DATETIME DEFAULT NULL COMMENT '最近登录时间',
    ADD COLUMN last_login_ip VARCHAR(64) DEFAULT NULL COMMENT '最近登录IP',
    ADD COLUMN login_count INT NOT NULL DEFAULT 0 COMMENT '成功登录次数';

CREATE TABLE IF NOT EXISTS sys_login_log (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id        BIGINT       DEFAULT NULL COMMENT '用户ID，未知账号登录时为空',
    username       VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    login_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    login_ip       VARCHAR(64)  DEFAULT NULL COMMENT '登录IP',
    user_agent     VARCHAR(500) DEFAULT NULL COMMENT '浏览器信息',
    success        TINYINT      NOT NULL DEFAULT 0 COMMENT '结果：1-成功 0-失败',
    failure_reason VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    PRIMARY KEY (id),
    KEY idx_login_user_id (user_id),
    KEY idx_login_time (login_time),
    KEY idx_login_success (success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户登录日志表';
