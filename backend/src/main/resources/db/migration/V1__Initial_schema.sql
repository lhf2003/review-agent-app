-- ================================================
-- Review Agent 数据库初始迁移脚本
-- Flyway Version: V1
-- Description: 合并所有历史 SQL 脚本为初始版本
-- ================================================

-- ================================================
-- Part 1: 基础表结构
-- ================================================

CREATE TABLE IF NOT EXISTS analysis_result
(
    id                BIGINT AUTO_INCREMENT  NOT NULL,
    file_id           BIGINT                 NOT NULL COMMENT '文件数据ID',
    vector_id         VARCHAR(255)           NULL COMMENT '向量ID',
    user_id           BIGINT                 NOT NULL COMMENT '用户id',
    problem_statement VARCHAR(255)           NULL COMMENT '用户的问题描述',
    solution          LONGTEXT               NULL COMMENT 'AI回复的解决方案',
    session_start     TINYINT                NULL COMMENT '会话开始索引',
    session_end       TINYINT                NULL COMMENT '会话结束索引',
    session_content   LONGTEXT               NULL COMMENT '会话内容',
    status            TINYINT                NOT NULL COMMENT '状态（0=失败 1=成功）',
    created_time      datetime DEFAULT NOW() NULL COMMENT '创建时间',
    deleted           TINYINT(1) DEFAULT 0   NOT NULL COMMENT '删除标记（0-未删除 1-已删除）',
    deleted_at        DATETIME               NULL COMMENT '删除时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id),
    CONSTRAINT fk_analysis_result_user FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    CONSTRAINT fk_analysis_result_data FOREIGN KEY (file_id) REFERENCES data_info(id) ON DELETE CASCADE
) COMMENT '分析结果表';

CREATE TABLE IF NOT EXISTS data_info
(
    id               BIGINT AUTO_INCREMENT  NOT NULL,
    user_id          BIGINT   DEFAULT 1     NULL COMMENT '用户ID',
    file_name        VARCHAR(100)           NOT NULL COMMENT '文件名（日期+时间）',
    file_content     LONGTEXT               NULL COMMENT '文件内容',
    source           INT         DEFAULT 0  COMMENT '数据来源 (0=LOCAL, 1=GEMINI, 2=CHATGPT)',
    processed_status TINYINT  DEFAULT 0     NULL COMMENT '处理状态（0=未分析, 1=正在分析 2=已分析 3=有更新 4=分析失败）',
    created_time     datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time      datetime               NULL COMMENT '文件上一次修改时间',
    deleted          TINYINT(1) DEFAULT 0   NOT NULL COMMENT '删除标记（0-未删除 1-已删除）',
    deleted_at       DATETIME               NULL COMMENT '删除时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id),
    CONSTRAINT fk_data_info_user FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE
) COMMENT ='文件信息（模型对话数据）';

CREATE TABLE IF NOT EXISTS llm_provider
(
    id          INT AUTO_INCREMENT NOT NULL,
    name        VARCHAR(20)        NULL COMMENT '提供商名称',
    request_url VARCHAR(200)       NULL COMMENT '请求地址',
    api_key     VARCHAR(255)       NOT NULL COMMENT 'APIKEY',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='模型提供商';

CREATE TABLE IF NOT EXISTS report_data
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    user_id        BIGINT                NOT NULL,
    report_content LONGTEXT              NOT NULL COMMENT '报告内容',
    type           TINYINT               NULL COMMENT '报告类型（1日报 2周报）',
    start_date     date                  NULL COMMENT '开始时间',
    end_date       date                  NULL COMMENT '结束时间',
    create_time    datetime              NULL COMMENT '生成时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id),
    CONSTRAINT fk_report_data_user FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE
) COMMENT ='日/周报记录';

CREATE TABLE IF NOT EXISTS sync_record
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    user_id     BIGINT                NOT NULL COMMENT '用户id',
    spend_time  DOUBLE(4, 1)          NOT NULL COMMENT '同步耗时',
    sync_count  INT                   NOT NULL COMMENT '本次同步的文件总量',
    create_time datetime              NULL COMMENT '同步时间',
    status      INT      DEFAULT 0    NOT NULL COMMENT '同步状态：0=成功, 1=同步中, 2=失败',
    message     VARCHAR(500)          NULL COMMENT '同步消息或错误描述',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id),
    CONSTRAINT fk_sync_record_user FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE
) COMMENT ='文件同步记录';

CREATE TABLE IF NOT EXISTS user_config
(
    id                       BIGINT AUTO_INCREMENT        NOT NULL,
    user_id                  BIGINT                       NOT NULL COMMENT '关联用户id',
    scan_directory           VARCHAR(255)                 NULL COMMENT '扫描路径',
    auto_scan_enabled        TINYINT(1)  DEFAULT 1        NULL COMMENT '是否自动扫描',
    scan_interval_seconds    INT         DEFAULT 30       NULL COMMENT '扫描间隔（秒）',
    llm_provider             VARCHAR(50) DEFAULT 'openai' NULL COMMENT 'LLM 提供商',
    tag_llm_provider         INT                          NULL,
    openai_api_key_encrypted VARCHAR(255)                 NULL COMMENT 'API 密钥（加密存储）',
    update_time              timestamp   DEFAULT NOW()    NULL COMMENT '更新时间',
    session_llm_provider     INT                          NULL,
    daily_enabled            TINYINT                      NULL COMMENT '是否启用日报功能',
    daily_cron               VARCHAR(50)                  NULL COMMENT '指定日报的cron',
    weekly_enabled           TINYINT                      NULL COMMENT '是否启用周报功能',
    weekly_cron              VARCHAR(50)                  NULL COMMENT '指定周报的cron',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id),
    CONSTRAINT fk_user_config_user FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_info
(
    id          BIGINT AUTO_INCREMENT  NOT NULL COMMENT '用户ID',
    username    VARCHAR(50)            NULL COMMENT '用户名',
    password    VARCHAR(255)           NULL COMMENT '密码',
    email       VARCHAR(50)            NULL COMMENT '邮箱',
    phone       VARCHAR(50)            NULL COMMENT '手机号',
    avatar      VARCHAR(255)           NULL COMMENT '头像',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time datetime DEFAULT NOW() NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS analysis_collection
(
    id           bigint       NOT NULL AUTO_INCREMENT,
    user_id      bigint       NOT NULL COMMENT '归属用户ID',
    name         varchar(128) NOT NULL COMMENT '合集名称',
    description  varchar(512) DEFAULT NULL COMMENT '合集描述（可选）',
    created_time datetime     DEFAULT CURRENT_TIMESTAMP,
    updated_time datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT(1)   DEFAULT 0 NOT NULL COMMENT '删除标记（0-未删除 1-已删除）',
    deleted_at   DATETIME     DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT fk_analysis_collection_user FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='分析结果合集表';

CREATE TABLE IF NOT EXISTS collection_relation
(
    id                 bigint NOT NULL AUTO_INCREMENT,
    collection_id      bigint NOT NULL COMMENT '合集ID',
    analysis_result_id bigint NOT NULL COMMENT '分析结果ID',
    created_time       datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_col_res` (`collection_id`, `analysis_result_id`),
    KEY `idx_analysis_result_id` (`analysis_result_id`),
    CONSTRAINT fk_collection_relation_collection FOREIGN KEY (collection_id) REFERENCES analysis_collection(id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='合集与分析结果关联表';

CREATE TABLE IF NOT EXISTS quiz_record
(
    id                      bigint       NOT NULL AUTO_INCREMENT,
    user_id                 bigint       NOT NULL COMMENT '用户ID',
    collection_id           bigint       NOT NULL COMMENT '来源合集ID',
    total_score             int          DEFAULT 0 COMMENT '总分',
    status                  tinyint      DEFAULT 0 COMMENT '状态 (0=进行中, 1=已完成)',
    created_time            datetime     DEFAULT CURRENT_TIMESTAMP,
    updated_time            datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    submit_time             DATETIME     DEFAULT NULL COMMENT '做题时间（用户提交答案的时间）',
    collection_version_hash VARCHAR(64)  COMMENT '合集内容哈希值（用于版本检测）',
    analysis_result_ids     TEXT         COMMENT '生成题库时使用的分析结果ID列表（JSON格式）',
    is_outdated             BOOLEAN      DEFAULT FALSE COMMENT '题型是否已过期（合集有新内容）',
    deleted                 TINYINT(1)   DEFAULT 0 NOT NULL COMMENT '删除标记（0-未删除 1-已删除）',
    deleted_at              DATETIME     DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_col` (`user_id`, `collection_id`),
    CONSTRAINT fk_quiz_record_user FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    CONSTRAINT fk_quiz_record_collection FOREIGN KEY (collection_id) REFERENCES analysis_collection(id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI测验会话记录表';

CREATE TABLE IF NOT EXISTS quiz_question
(
    id                  bigint        NOT NULL AUTO_INCREMENT,
    quiz_id             bigint        NOT NULL COMMENT '所属测验ID',
    related_analysis_id bigint        DEFAULT NULL COMMENT '关联的原始分析ID (可溯源)',
    question_text       text          NOT NULL COMMENT '题干',
    options_json        json          NOT NULL COMMENT '选项 (JSON数组)',
    correct_answer      varchar(255)  NOT NULL COMMENT '正确答案',
    explanation         text          DEFAULT NULL COMMENT '解析',
    user_answer         varchar(255)  DEFAULT NULL COMMENT '用户选择的答案',
    is_correct          tinyint(1)    DEFAULT NULL COMMENT '是否回答正确',
    created_time        datetime      DEFAULT CURRENT_TIMESTAMP,
    question_type       VARCHAR(20)   NOT NULL DEFAULT 'single_choice' COMMENT '题目类型 (single_choice-单选题, multiple_choice-多选题, true_false-判断题, fill_blank-填空题, code_snippet-代码识别题)',
    difficulty_level    TINYINT       DEFAULT 3 COMMENT '难度等级 (1-非常简单, 2-简单, 3-中等, 4-困难, 5-非常困难)',
    knowledge_point     VARCHAR(100)  COMMENT '知识点标签（用于掌握度分析）',
    time_limit          INT           DEFAULT 60 COMMENT '答题时限（秒）',
    answer_count        INT           DEFAULT 0 COMMENT '被回答次数（用于题目质量评估）',
    correct_count       INT           DEFAULT 0 COMMENT '正确次数（用于难度校准）',
    PRIMARY KEY (`id`),
    KEY `idx_quiz_id` (`quiz_id`),
    CONSTRAINT fk_quiz_question_record FOREIGN KEY (quiz_id) REFERENCES quiz_record(id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI测验题目表';

CREATE TABLE IF NOT EXISTS default_llm_provider
(
    id   int auto_increment primary key,
    name varchar(20)  null,
    url  varchar(255) null
) comment '默认模型提供商';

CREATE TABLE IF NOT EXISTS user_llm_config
(
    id           bigint auto_increment primary key,
    user_id      bigint       not null comment '用户id',
    name         varchar(20)  not null,
    url          varchar(255) null,
    api_key      varchar(255) null,
    is_connected tinyint(1)   null comment '是否已连接',
    CONSTRAINT fk_user_llm_config_user FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE
) comment '用户模型服务商配置';

CREATE TABLE IF NOT EXISTS selected_model
(
    id             int auto_increment primary key,
    provider_id    int          not null comment '绑定模型提供商id',
    user_id        bigint       not null comment '用户Id',
    model_name     varchar(100) not null comment '模型名称',
    model_capacity int          null comment '模型能力',
    CONSTRAINT fk_selected_model_user FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE
) comment '已选择的模型';

CREATE TABLE IF NOT EXISTS achievement_definition
(
    id               int auto_increment primary key,
    code             varchar(50)  not null unique comment '成就代码',
    name             varchar(100) not null comment '成就名称',
    description      varchar(500) not null comment '成就描述',
    icon             varchar(50)  not null comment '成就图标',
    condition_type   varchar(50)  not null comment '条件类型(count_sync, count_analysis, count_collection, count_quiz, continuous_days)',
    condition_value  int          not null comment '条件值',
    category         varchar(50)  not null comment '成就分类(milestone, activity, knowledge, special)',
    order_index      int          null comment '排序索引',
    created_time     datetime     default current_timestamp null
) comment '成就定义表';

CREATE TABLE IF NOT EXISTS user_achievement
(
    id               bigint auto_increment primary key,
    user_id          bigint       not null comment '用户ID',
    achievement_code varchar(50)  not null comment '成就代码',
    unlocked         tinyint(1)   default 0 null comment '是否解锁',
    progress         int          default 0 null comment '当前进度',
    unlocked_time    datetime     null comment '解锁时间',
    created_time     datetime     default current_timestamp null,
    constraint uk_user_achievement
        unique (user_id, achievement_code),
    CONSTRAINT fk_user_achievement_user FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE
) comment '用户成就表';

-- 插入成就定义数据
INSERT INTO achievement_definition (code, name, description, icon, condition_type, condition_value, category, order_index) VALUES
('first_sync', '初出茅庐', '完成第一次文件同步', 'Document', 'count_sync', 1, 'milestone', 1),
('first_analysis', '初露锋芒', '完成第一次问题分析', 'Edit', 'count_analysis', 1, 'milestone', 2),
('first_collection', '初建知识', '创建第一个知识合集', 'FolderOpened', 'count_collection', 1, 'milestone', 3),
('first_quiz', '小试牛刀', '完成第一次测验', 'CircleCheck', 'count_quiz', 1, 'milestone', 4),
('quiz_master_5', '学习达人', '完成5次测验', 'Star', 'count_quiz', 5, 'activity', 5),
('quiz_master_10', '学习专家', '完成10次测验', 'StarFilled', 'count_quiz', 10, 'activity', 6),
('collector_5', '知识收集者', '创建5个合集', 'Folder', 'count_collection', 5, 'activity', 7),
('collector_10', '知识大师', '创建10个合集', 'FolderOpened', 'count_collection', 10, 'activity', 8),
('continuous_7', '坚持不懈', '连续学习7天', 'Calendar', 'continuous_days', 7, 'activity', 9),
('continuous_30', '持之以恒', '连续学习30天', 'CalendarFilled', 'continuous_days', 30, 'activity', 10),
('tag_master_10', '标签专家', '掌握10个知识点', 'PriceTag', 'count_analysis', 10, 'knowledge', 11),
('quiz_perfect_100', '完美主义者', '单次测验满分', 'Trophy', 'count_quiz', 1, 'knowledge', 12),
('java_master', 'Java高手', '掌握10个Java知识点', 'DataLine', 'count_analysis', 10, 'knowledge', 13),
('python_master', 'Python达人', '掌握10个Python知识点', 'ChatDotRound', 'count_analysis', 10, 'knowledge', 14),
('js_master', 'JavaScript专家', '掌握10个JavaScript知识点', 'Coin', 'count_analysis', 10, 'knowledge', 15),
('continuous_90', '百日铸剑', '连续学习90天', 'Medal', 'continuous_days', 90, 'special', 16),
('sync_master_10', '数据先锋', '同步10次文件', 'Upload', 'count_sync', 10, 'special', 17),
('sync_master_50', '数据巨匠', '同步50次文件', 'UploadFilled', 'count_sync', 50, 'special', 18),
('analysis_master_20', '分析达人', '分析20个问题', 'TrendCharts', 'count_analysis', 20, 'special', 19),
('all_rounder', '全能选手', '解锁所有里程碑成就', 'Crown', 'count_analysis', 4, 'special', 20);

-- ================================================
-- Part 2: 用户默认模型配置表
-- ================================================

CREATE TABLE IF NOT EXISTS user_default_model_config
(
    id          BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键ID',
    user_id     BIGINT               NOT NULL COMMENT '用户ID',
    model_type   VARCHAR(50)          NOT NULL COMMENT '模型类型：SESSION_SPLIT(会话拆分), TAG_CLASSIFICATION(标签分类), SMART_ANALYSIS(智能分析)',
    provider_id  INT                  NULL COMMENT '模型提供商ID（关联 user_llm_config 表）',
    model_name   VARCHAR(100)         NOT NULL COMMENT '选中的模型名称',
    created_time DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_model_type` (`user_id`, `model_type`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_provider_id` (`provider_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户默认模型配置表';

-- ================================================
-- Part 3: 练习题功能增强
-- ================================================

CREATE TABLE IF NOT EXISTS quiz_mistake (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    quiz_id BIGINT COMMENT '来源测验ID（可溯源）',
    mistake_count INT DEFAULT 1 COMMENT '错误次数',
    last_mistake_time DATETIME COMMENT '最后一次错误时间',
    mastered BOOLEAN DEFAULT FALSE COMMENT '是否已掌握（连续答对3次标记为掌握）',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    snoozed_until DATETIME COMMENT '延迟复习截止时间(稍后复习按钮触发后设置)',
    snooze_count INT DEFAULT 0 COMMENT '延迟复习次数(最多3次)',
    PRIMARY KEY (id),
    INDEX `idx_user_mastered` (`user_id`, `mastered`),
    INDEX `idx_question` (`question_id`),
    INDEX `idx_last_mistake` (`user_id`, `last_mistake_time`),
    INDEX `idx_mistake_snoozed` (`user_id`, `snoozed_until`, `mastered`),
    CONSTRAINT `fk_mistake_user`
        FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mistake_question`
        FOREIGN KEY (`question_id`) REFERENCES `quiz_question` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mistake_quiz`
        FOREIGN KEY (`quiz_id`) REFERENCES `quiz_record` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '错题本表（记录用户答错的题目及掌握状态）';

CREATE TABLE IF NOT EXISTS knowledge_mastery (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID（与tag表关联）',
    knowledge_point VARCHAR(100) NOT NULL COMMENT '知识点',
    total_answered INT DEFAULT 0 COMMENT '总答题次数',
    correct_count INT DEFAULT 0 COMMENT '正确次数',
    mastery_score DECIMAL(5,2) DEFAULT 0 COMMENT '掌握度 (0-100)',
    average_time INT COMMENT '平均答题时间（秒）',
    last_practice_time DATETIME COMMENT '最后一次练习时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_knowledge` (`user_id`, `knowledge_point`),
    UNIQUE KEY `uk_user_knowledge_point` (`user_id`, `knowledge_point`),
    INDEX `idx_mastery_score` (`user_id`, `mastery_score`),
    INDEX `idx_last_practice` (`user_id`, `last_practice_time`),
    INDEX `idx_knowledge_mastery_user_id` (`user_id`),
    INDEX `idx_knowledge_mastery_knowledge_point` (`knowledge_point`),
    CONSTRAINT `fk_mastery_user`
        FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '知识点掌握度表（追踪用户对各知识点的掌握程度）';

CREATE INDEX idx_knowledge_mastery_tag_id ON knowledge_mastery(tag_id);
CREATE INDEX idx_knowledge_mastery_user_tag ON knowledge_mastery(user_id, tag_id);
CREATE INDEX idx_quiz_question_type ON quiz_question(question_type);
CREATE INDEX idx_quiz_question_difficulty ON quiz_question(difficulty_level);
CREATE INDEX idx_quiz_question_knowledge ON quiz_question(knowledge_point);
CREATE INDEX idx_quiz_quiz_difficulty ON quiz_question(quiz_id, difficulty_level);
CREATE INDEX idx_quiz_user_knowledge ON quiz_question(knowledge_point, difficulty_level);

-- ================================================
-- Part 4: 版本追踪索引
-- ================================================

CREATE INDEX idx_quiz_collection_version ON quiz_record(collection_id, is_outdated);

-- ================================================
-- Part 5: 软删除索引
-- ================================================

CREATE INDEX idx_data_info_deleted ON data_info(deleted);
CREATE INDEX idx_data_info_user_deleted ON data_info(user_id, deleted);
CREATE INDEX idx_analysis_result_deleted ON analysis_result(deleted);
CREATE INDEX idx_analysis_result_user_deleted ON analysis_result(user_id, deleted);
CREATE INDEX idx_analysis_collection_deleted ON analysis_collection(deleted);
CREATE INDEX idx_analysis_collection_user_deleted ON analysis_collection(user_id, deleted);
CREATE INDEX idx_quiz_record_deleted ON quiz_record(deleted);
CREATE INDEX idx_quiz_record_user_deleted ON quiz_record(user_id, deleted);
CREATE INDEX idx_quiz_record_collection_deleted ON quiz_record(collection_id, deleted);

-- ================================================
-- Part 6: 外键约束和索引
-- ================================================

-- tag_relation 的外键约束（表定义在后面 Part 13）
-- 在 tag 和 tag_dimension 表创建后添加

-- 复合索引
CREATE INDEX idx_data_info_user_created ON data_info(user_id, created_time DESC);
CREATE INDEX idx_analysis_result_user_created ON analysis_result(user_id, created_time DESC);
CREATE INDEX idx_analysis_result_file_status ON analysis_result(file_id, status);
CREATE INDEX idx_analysis_collection_user_updated ON analysis_collection(user_id, updated_time DESC);
CREATE INDEX idx_collection_relation_collection_created ON collection_relation(collection_id, created_time DESC);
CREATE INDEX idx_quiz_record_user_created ON quiz_record(user_id, created_time DESC);
CREATE INDEX idx_quiz_record_collection_created ON quiz_record(collection_id, created_time DESC);
CREATE INDEX idx_quiz_record_user_collection ON quiz_record(user_id, collection_id, created_time DESC);
CREATE INDEX idx_quiz_question_quiz_id ON quiz_question(quiz_id);
CREATE INDEX idx_sync_record_user_created ON sync_record(user_id, create_time DESC);
CREATE INDEX idx_report_data_user_type ON report_data(user_id, type, start_date DESC);

-- 单列索引
CREATE INDEX idx_user_username ON user_info(username);
CREATE INDEX idx_user_email ON user_info(email);
CREATE INDEX idx_data_info_file_name ON data_info(file_name);
CREATE INDEX idx_data_info_processed_status ON data_info(processed_status);
CREATE INDEX idx_data_info_source ON data_info(source);
CREATE INDEX idx_analysis_result_problem_statement ON analysis_result(problem_statement(255));
CREATE INDEX idx_analysis_result_created_time ON analysis_result(created_time);
CREATE INDEX idx_quiz_question_related_analysis_id ON quiz_question(related_analysis_id);
CREATE INDEX idx_user_achievement_code ON user_achievement(achievement_code);
CREATE INDEX idx_user_achievement_unlocked ON user_achievement(unlocked);
CREATE INDEX idx_user_achievement_unlocked_time ON user_achievement(unlocked_time);

-- ================================================
-- Part 7: 同步记录状态索引
-- ================================================

CREATE INDEX idx_sync_record_user_status ON sync_record(user_id, status);

-- ================================================
-- Part 8: 通知设置表
-- ================================================

CREATE TABLE IF NOT EXISTS notification_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    browser_notification_enabled TINYINT(1) DEFAULT 0 COMMENT '是否启用浏览器通知',
    reminder_frequency_days INT DEFAULT 1 COMMENT '提醒频率（天数）',
    reminder_hour INT DEFAULT 9 COMMENT '提醒时间（小时，0-23）',
    mistake_review_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用错题复习提醒',
    quiz_completion_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用习题完成提醒',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知设置表';

-- ================================================
-- Part 9: 错题历史记录表
-- ================================================

CREATE TABLE IF NOT EXISTS quiz_mistake_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '历史记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    mistake_id BIGINT NOT NULL COMMENT '错题记录ID（关联quiz_mistake表）',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    quiz_id BIGINT COMMENT '来源测验ID',
    wrong_answer VARCHAR(500) COMMENT '错误答案',
    correct_answer VARCHAR(500) COMMENT '正确答案',
    time_spent INT COMMENT '答题用时（秒）',
    is_correct TINYINT(1) DEFAULT 0 COMMENT '是否正确（0=错误，1=正确）',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '答题时间',
    INDEX idx_user_mistake (user_id, mistake_id),
    INDEX idx_question (question_id),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='错题答题历史记录表';

-- ================================================
-- Part 10: 测验提交时间索引
-- ================================================

CREATE INDEX idx_submit_time ON quiz_record(submit_time);
CREATE INDEX idx_user_submit_time ON quiz_record(user_id, submit_time);

-- ================================================
-- Part 11: 思维范式沉淀重构
-- ================================================

-- 维度定义表
CREATE TABLE IF NOT EXISTS tag_dimension (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '维度名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '维度编码',
    description VARCHAR(200) COMMENT '维度说明',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code (code)
) COMMENT='标签维度定义';

-- 标签表（支持无限层级+多维度）
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '标签名称',
    dimension_id BIGINT NOT NULL COMMENT '所属维度',
    parent_id BIGINT NULL COMMENT '父标签ID，null为根',
    level INT DEFAULT 1 COMMENT '层级深度(1-4)',
    path VARCHAR(500) COMMENT '完整路径，如：/编程/Java/Spring',
    paradigm_code VARCHAR(50) COMMENT '范式编码：DECOMPOSITION/ANALOGY等',
    description TEXT COMMENT '范式说明',
    when_to_use TEXT COMMENT '适用场景描述',
    example TEXT COMMENT '典型案例',
    user_id BIGINT COMMENT '用户ID（系统内置时可为null）',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (dimension_id) REFERENCES tag_dimension(id),
    FOREIGN KEY (parent_id) REFERENCES tag(id),
    INDEX idx_dimension (dimension_id),
    INDEX idx_parent (parent_id),
    INDEX idx_user (user_id),
    INDEX idx_path (path),
    INDEX idx_paradigm_code (paradigm_code)
) COMMENT='标签表（支持无限层级和多维度）';

-- 标签关系表
CREATE TABLE IF NOT EXISTS tag_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_tag_id BIGINT NOT NULL COMMENT '源标签',
    target_tag_id BIGINT NOT NULL COMMENT '目标标签',
    relation_type VARCHAR(50) NOT NULL COMMENT '关系类型：DEPENDS_ON/SIMILAR_TO/COMPLEMENTS/CONFLICTS_WITH/EVOLVES_TO',
    strength INT DEFAULT 50 COMMENT '关系强度1-100',
    evidence TEXT COMMENT '关系依据说明',
    is_auto_detected BOOLEAN DEFAULT FALSE COMMENT '是否AI自动发现',
    user_id BIGINT COMMENT '用户ID（系统预设关系可为null）',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (source_tag_id) REFERENCES tag(id) ON DELETE CASCADE,
    FOREIGN KEY (target_tag_id) REFERENCES tag(id) ON DELETE CASCADE,
    CONSTRAINT fk_tag_relation_user FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE,
    UNIQUE KEY uk_relation (source_tag_id, target_tag_id, relation_type),
    INDEX idx_source (source_tag_id),
    INDEX idx_target (target_tag_id),
    INDEX idx_type (relation_type)
) COMMENT='标签关系表（新）';

-- 初始化维度数据
INSERT INTO tag_dimension (name, code, description, icon, sort_order) VALUES
('技术领域', 'TECH_DOMAIN', '技术栈和领域分类', 'Document', 1),
('思维范式', 'THINKING_PARADIGM', '思考和解决问题的方式', 'Cpu', 2),
('难度等级', 'DIFFICULTY', '知识难度分级', 'Histogram', 3),
('应用场景', 'SCENARIO', '知识适用的具体场景', 'OfficeBuilding', 4)
ON DUPLICATE KEY UPDATE updated_time = CURRENT_TIMESTAMP;

-- 初始化思维范式标签
INSERT INTO tag (name, dimension_id, parent_id, level, path, paradigm_code, description, when_to_use, example, user_id, created_time)
SELECT '问题分解', (SELECT id FROM tag_dimension WHERE code = 'THINKING_PARADIGM'), NULL, 1, '/思维范式/问题分解',
    'DECOMPOSITION', '将复杂问题拆解成多个可管理的小问题', '面对复杂系统、大规模项目或难以直接解决的任务时使用',
    '微服务架构设计：先分解业务域，再逐个设计服务', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'DECOMPOSITION');

INSERT INTO tag (name, dimension_id, parent_id, level, path, paradigm_code, description, when_to_use, example, user_id, created_time)
SELECT '类比推理', (SELECT id FROM tag_dimension WHERE code = 'THINKING_PARADIGM'), NULL, 1, '/思维范式/类比推理',
    'ANALOGY', '用已知领域的知识解释未知领域的问题', '学习新概念、向非技术人员解释技术问题、跨领域创新时',
    '用餐厅点餐解释消息队列：顾客(生产者)-订单(消息)-厨房(消费者)', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'ANALOGY');

INSERT INTO tag (name, dimension_id, parent_id, level, path, paradigm_code, description, when_to_use, example, user_id, created_time)
SELECT '第一性原理', (SELECT id FROM tag_dimension WHERE code = 'THINKING_PARADIGM'), NULL, 1, '/思维范式/第一性原理',
    'FIRST_PRINCIPLES', '从最基本的真理出发，通过逻辑推理构建解决方案', '创新突破、打破常规思维、优化现有方案时',
    '电池成本优化：不认可"电池就是贵"，拆解原材料成本发现可降低80%', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'FIRST_PRINCIPLES');

INSERT INTO tag (name, dimension_id, parent_id, level, path, paradigm_code, description, when_to_use, example, user_id, created_time)
SELECT '模式识别', (SELECT id FROM tag_dimension WHERE code = 'THINKING_PARADIGM'), NULL, 1, '/思维范式/模式识别',
    'PATTERN_RECOGNITION', '识别问题中的重复模式和规律', '代码重构、系统优化、设计模式应用时',
    '发现多个类都有相似的初始化代码，提取为抽象基类', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'PATTERN_RECOGNITION');

INSERT INTO tag (name, dimension_id, parent_id, level, path, paradigm_code, description, when_to_use, example, user_id, created_time)
SELECT '系统调试', (SELECT id FROM tag_dimension WHERE code = 'THINKING_PARADIGM'), NULL, 1, '/思维范式/系统调试',
    'SYSTEMATIC_DEBUGGING', '系统化地定位和解决问题', '遇到BUG、系统故障、程序异常时',
    '分半法排查：先确定是前端还是后端问题，再逐层深入', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'SYSTEMATIC_DEBUGGING');

INSERT INTO tag (name, dimension_id, parent_id, level, path, paradigm_code, description, when_to_use, example, user_id, created_time)
SELECT '权衡分析', (SELECT id FROM tag_dimension WHERE code = 'THINKING_PARADIGM'), NULL, 1, '/思维范式/权衡分析',
    'TRADE_OFF_ANALYSIS', '分析多种方案的利弊，做出最优选择', '技术选型、架构决策、资源分配时',
    '数据库选型：MySQL(成熟) vs MongoDB(灵活)，根据一致性需求选择', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'TRADE_OFF_ANALYSIS');

INSERT INTO tag (name, dimension_id, parent_id, level, path, paradigm_code, description, when_to_use, example, user_id, created_time)
SELECT '抽象建模', (SELECT id FROM tag_dimension WHERE code = 'THINKING_PARADIGM'), NULL, 1, '/思维范式/抽象建模',
    'ABSTRACTION_MODELING', '从具体实例中提取通用模型', '框架设计、领域建模、通用组件开发时',
    '从多个订单处理流程中抽象出工作流引擎', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'ABSTRACTION_MODELING');

-- 初始化难度等级标签
INSERT INTO tag (name, dimension_id, parent_id, level, path, user_id, created_time)
SELECT '入门', (SELECT id FROM tag_dimension WHERE code = 'DIFFICULTY'), NULL, 1, '/难度等级/入门', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag t JOIN tag_dimension td ON t.dimension_id = td.id WHERE td.code = 'DIFFICULTY' AND t.name = '入门');

INSERT INTO tag (name, dimension_id, parent_id, level, path, user_id, created_time)
SELECT '初级', (SELECT id FROM tag_dimension WHERE code = 'DIFFICULTY'), NULL, 1, '/难度等级/初级', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag t JOIN tag_dimension td ON t.dimension_id = td.id WHERE td.code = 'DIFFICULTY' AND t.name = '初级');

INSERT INTO tag (name, dimension_id, parent_id, level, path, user_id, created_time)
SELECT '中级', (SELECT id FROM tag_dimension WHERE code = 'DIFFICULTY'), NULL, 1, '/难度等级/中级', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag t JOIN tag_dimension td ON t.dimension_id = td.id WHERE td.code = 'DIFFICULTY' AND t.name = '中级');

INSERT INTO tag (name, dimension_id, parent_id, level, path, user_id, created_time)
SELECT '高级', (SELECT id FROM tag_dimension WHERE code = 'DIFFICULTY'), NULL, 1, '/难度等级/高级', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag t JOIN tag_dimension td ON t.dimension_id = td.id WHERE td.code = 'DIFFICULTY' AND t.name = '高级');

INSERT INTO tag (name, dimension_id, parent_id, level, path, user_id, created_time)
SELECT '专家', (SELECT id FROM tag_dimension WHERE code = 'DIFFICULTY'), NULL, 1, '/难度等级/专家', NULL, CURRENT_TIMESTAMP
FROM dual WHERE NOT EXISTS (SELECT 1 FROM tag t JOIN tag_dimension td ON t.dimension_id = td.id WHERE td.code = 'DIFFICULTY' AND t.name = '专家');

-- 新知识点-标签关联表
CREATE TABLE IF NOT EXISTS analysis_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    analysis_result_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE COMMENT '是否主标签',
    confidence INT DEFAULT 100 COMMENT 'AI匹配置信度1-100',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (analysis_result_id) REFERENCES analysis_result(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE CASCADE,
    UNIQUE KEY uk_analysis_tag (analysis_result_id, tag_id),
    INDEX idx_analysis (analysis_result_id),
    INDEX idx_tag (tag_id)
) COMMENT='分析结果-标签关联表（新版）';

-- 初始化思维范式关系
INSERT INTO tag_relation (source_tag_id, target_tag_id, relation_type, strength, evidence, is_auto_detected, user_id)
SELECT (SELECT id FROM tag WHERE paradigm_code = 'TRADE_OFF_ANALYSIS'), (SELECT id FROM tag WHERE paradigm_code = 'DECOMPOSITION'),
    'DEPENDS_ON', 80, '权衡多个方案需要先能分解问题', TRUE, NULL
FROM dual WHERE EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'TRADE_OFF_ANALYSIS') AND EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'DECOMPOSITION')
AND NOT EXISTS (SELECT 1 FROM tag_relation WHERE source_tag_id = (SELECT id FROM tag WHERE paradigm_code = 'TRADE_OFF_ANALYSIS')
    AND target_tag_id = (SELECT id FROM tag WHERE paradigm_code = 'DECOMPOSITION'));

INSERT INTO tag_relation (source_tag_id, target_tag_id, relation_type, strength, evidence, is_auto_detected, user_id)
SELECT (SELECT id FROM tag WHERE paradigm_code = 'ABSTRACTION_MODELING'), (SELECT id FROM tag WHERE paradigm_code = 'PATTERN_RECOGNITION'),
    'DEPENDS_ON', 85, '抽象建模需要先识别重复模式', TRUE, NULL
FROM dual WHERE EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'ABSTRACTION_MODELING') AND EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'PATTERN_RECOGNITION')
AND NOT EXISTS (SELECT 1 FROM tag_relation WHERE source_tag_id = (SELECT id FROM tag WHERE paradigm_code = 'ABSTRACTION_MODELING')
    AND target_tag_id = (SELECT id FROM tag WHERE paradigm_code = 'PATTERN_RECOGNITION'));

INSERT INTO tag_relation (source_tag_id, target_tag_id, relation_type, strength, evidence, is_auto_detected, user_id)
SELECT (SELECT id FROM tag WHERE paradigm_code = 'SYSTEMATIC_DEBUGGING'), (SELECT id FROM tag WHERE paradigm_code = 'ANALOGY'),
    'COMPLEMENTS', 70, '调试时可以用类比快速定位问题', TRUE, NULL
FROM dual WHERE EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'SYSTEMATIC_DEBUGGING') AND EXISTS (SELECT 1 FROM tag WHERE paradigm_code = 'ANALOGY')
AND NOT EXISTS (SELECT 1 FROM tag_relation WHERE source_tag_id = (SELECT id FROM tag WHERE paradigm_code = 'SYSTEMATIC_DEBUGGING')
    AND target_tag_id = (SELECT id FROM tag WHERE paradigm_code = 'ANALOGY'));

-- analysis_tag 索引（在表创建后添加）
CREATE INDEX idx_analysis_tag_tag_id ON analysis_tag(tag_id);

-- ================================================
-- 迁移完成
-- ================================================
