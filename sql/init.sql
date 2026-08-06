-- =====================================================
-- 煤层瓦斯智能分析平台 数据库初始化脚本
-- Database: meitan
-- =====================================================

CREATE DATABASE IF NOT EXISTS meitan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE meitan;

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name   VARCHAR(50)  DEFAULT NULL COMMENT '真实姓名',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN / USER',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-启用 0-禁用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 数据文件表
-- ----------------------------
DROP TABLE IF EXISTS data_file;
CREATE TABLE data_file (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    module_type VARCHAR(20)  NOT NULL COMMENT '模块：ANALYSIS / STATISTICS / DETECTION',
    file_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path   VARCHAR(500) NOT NULL COMMENT '服务器存储路径',
    file_size   BIGINT       DEFAULT 0 COMMENT '文件大小（字节）',
    upload_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_module_type (module_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据文件表';

-- ----------------------------
-- 计算任务表
-- ----------------------------
DROP TABLE IF EXISTS task_calculation;
CREATE TABLE task_calculation (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    module_type VARCHAR(20)  NOT NULL COMMENT '模块：ANALYSIS / STATISTICS / DETECTION',
    file_id     BIGINT       DEFAULT NULL COMMENT '关联数据文件ID',
    params_json TEXT         COMMENT '计算参数JSON',
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/SUCCESS/FAILED',
    error_msg   TEXT         DEFAULT NULL COMMENT '错误信息',
    start_time  DATETIME     DEFAULT NULL COMMENT '开始时间',
    end_time    DATETIME     DEFAULT NULL COMMENT '结束时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_module_type (module_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计算任务表';

-- ----------------------------
-- 板块一：吸附含量计算结果表
-- ----------------------------
DROP TABLE IF EXISTS calc_result_analysis;
CREATE TABLE calc_result_analysis (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id         BIGINT       NOT NULL COMMENT '关联任务ID',
    coal_type       VARCHAR(100) DEFAULT NULL COMMENT '煤型及编号',
    volatile        VARCHAR(20)  DEFAULT NULL COMMENT '挥发分(%)',
    vl              DOUBLE       DEFAULT NULL COMMENT 'Langmuir Vl值',
    pl              DOUBLE       DEFAULT NULL COMMENT 'Langmuir Pl值',
    p_array_json    LONGTEXT     COMMENT '压力数组JSON',
    vm_array_json   LONGTEXT     COMMENT '吸附量数组JSON',
    chart_style     VARCHAR(20)  DEFAULT 'curve' COMMENT '图表样式',
    stats_text      TEXT         COMMENT '统计摘要文本',
    chart_image     LONGTEXT     COMMENT '图表Base64图片',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='板块一计算结果表';

-- ----------------------------
-- 板块二：参数统计结果表
-- ----------------------------
DROP TABLE IF EXISTS calc_result_statistics;
CREATE TABLE calc_result_statistics (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id         BIGINT       NOT NULL COMMENT '关联任务ID',
    region_list_json TEXT        COMMENT '区域列表JSON',
    vol_filter      VARCHAR(50)  DEFAULT '全部' COMMENT '挥发分筛选条件',
    chart_type      VARCHAR(20)  DEFAULT 'scatter' COMMENT '图表类型',
    color_by        VARCHAR(50)  COMMENT '颜色编码字段',
    size_by         VARCHAR(50)  COMMENT '尺寸编码字段',
    stats_summary   TEXT         COMMENT '统计摘要',
    chart_image     LONGTEXT     COMMENT '图表Base64图片',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='板块二统计结果表';

-- ----------------------------
-- 板块三：突出危险性检测结果表
-- ----------------------------
DROP TABLE IF EXISTS calc_result_detection;
CREATE TABLE calc_result_detection (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    task_id         BIGINT       NOT NULL COMMENT '关联任务ID',
    source_desc     VARCHAR(500) COMMENT '数据来源描述',
    data_points     INT          COMMENT '数据点数',
    v_param         DOUBLE       COMMENT '孔隙容积V',
    temperature     DOUBLE       COMMENT '温度(°C)',
    a_param         DOUBLE       COMMENT '压缩系数A',
    crit_pressure   DOUBLE       COMMENT '临界压力(MPa)',
    crit_content    DOUBLE       COMMENT '临界含量(m³/t)',
    xy_array_json   LONGTEXT     COMMENT '游离瓦斯数组JSON',
    q_array_json    LONGTEXT     COMMENT '总瓦斯数组JSON',
    p_array_json    LONGTEXT     COMMENT '压力数组JSON',
    is_danger       TINYINT      DEFAULT 0 COMMENT '是否危险：1-是 0-否',
    danger_reason   TEXT         COMMENT '判定依据',
    chart_image     LONGTEXT     COMMENT '图表Base64图片',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='板块三检测结果表';

-- ----------------------------
-- 报告表
-- ----------------------------
DROP TABLE IF EXISTS report;
CREATE TABLE report (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    title       VARCHAR(200) NOT NULL COMMENT '报告标题',
    task_ids    VARCHAR(500) COMMENT '关联任务ID列表，逗号分隔',
    summary_json LONGTEXT    COMMENT '报告摘要JSON',
    file_path   VARCHAR(500) COMMENT '导出文件路径',
    file_format VARCHAR(10)  DEFAULT 'XLSX' COMMENT '格式：XLSX/HTML/PDF',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告表';

-- ----------------------------
-- 反馈表
-- ----------------------------
DROP TABLE IF EXISTS feedback;
CREATE TABLE feedback (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    rating      TINYINT      NOT NULL COMMENT '星级评分（1-5）',
    content     TEXT         COMMENT '反馈内容',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- ----------------------------
-- 新闻资讯表
-- ----------------------------
DROP TABLE IF EXISTS news;
CREATE TABLE news (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    title       VARCHAR(500) NOT NULL COMMENT '新闻标题',
    url         VARCHAR(500) DEFAULT NULL COMMENT '新闻链接',
    source      VARCHAR(100) DEFAULT NULL COMMENT '来源',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    status      TINYINT      DEFAULT 1 COMMENT '状态：1-显示 0-隐藏',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新闻资讯表';

-- ----------------------------
-- 初始数据
-- ----------------------------
INSERT INTO news (title, url, source, sort_order) VALUES
('国家矿山安全监察局：加强煤矿瓦斯防治工作', 'https://www.gov.cn/', '国家矿山安全监察局', 1),
('2024年全国煤矿瓦斯抽采利用率稳步提升', 'https://www.chinamine-safety.gov.cn/', '国家矿山安全监察局', 2),
('深部煤层瓦斯抽采技术取得新突破', 'https://www.chinamine-safety.gov.cn/', '行业动态', 3),
('煤矿瓦斯综合治理方案优化研究进展', 'https://www.gov.cn/', '行业动态', 4),
('智能化瓦斯监测预警系统在多个矿区推广应用', 'https://www.chinamine-safety.gov.cn/', '科技前沿', 5),
('煤层气开发利用十四五规划发布', 'https://www.gov.cn/', '政策法规', 6);

-- 默认管理员账号: admin / admin123（密码为明文，首次登录自动加密为BCrypt）
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
('admin', 'admin123', '系统管理员', 'ADMIN', 1);
