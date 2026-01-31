CREATE TABLE analysis_result
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
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

CREATE TABLE analysis_tag
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    analysis_id      BIGINT                NOT NULL COMMENT '分析结果ID',
    tag_id           INT                   NULL COMMENT '标签ID',
    sub_tag_id       VARCHAR(100)          NULL COMMENT '子标签Id列表',
    recommends       VARCHAR(100)          NULL COMMENT 'AI推荐的主标签',
    confidence_score DOUBLE                NULL COMMENT 'AI 判断置信度',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

CREATE TABLE data_info
(
    id               BIGINT AUTO_INCREMENT  NOT NULL,
    user_id          BIGINT   DEFAULT 1     NULL COMMENT '用户ID',
    file_name        VARCHAR(100)           NOT NULL COMMENT '文件名（日期+时间）',
    file_content     LONGTEXT               NULL COMMENT '文件内容',
    source           INT         DEFAULT 0  COMMENT '数据来源 (0=LOCAL, 1=GEMINI, 2=CHATGPT)',
    processed_status TINYINT  DEFAULT 0     NULL COMMENT '处理状态（0=未分析, 1=正在分析 2=已分析 3=有更新 4=分析失败）',
    created_time     datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time      datetime               NULL COMMENT '文件上一次修改时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件信息（模型对话数据）';

CREATE TABLE llm_provider
(
    id          INT AUTO_INCREMENT NOT NULL,
    name        VARCHAR(20)        NULL COMMENT '提供商名称',
    request_url VARCHAR(200)       NULL COMMENT '请求地址',
    api_key     VARCHAR(255)       NOT NULL COMMENT 'APIKEY',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='模型提供商';

CREATE TABLE main_tag
(
    id          INT UNSIGNED AUTO_INCREMENT NOT NULL COMMENT '主标签ID',
    user_id     BIGINT                      NOT NULL COMMENT '用户id',
    name        VARCHAR(50)                 NOT NULL COMMENT '主标签名称',
    create_time datetime DEFAULT NOW()      NULL,
    update_time datetime DEFAULT NOW()      NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='主分类标签表';

CREATE TABLE report_data
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    user_id        BIGINT                NOT NULL,
    report_content LONGTEXT              NOT NULL COMMENT '报告内容',
    type           TINYINT               NULL COMMENT '报告类型（1日报 2周报）',
    start_date     date                  NULL COMMENT '开始时间',
    end_date       date                  NULL COMMENT '结束时间',
    create_time    datetime              NULL COMMENT '生成时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='日/周报记录';

CREATE TABLE sub_tag
(
    id          INT UNSIGNED AUTO_INCREMENT NOT NULL COMMENT '子标签ID',
    user_id     BIGINT                      NOT NULL COMMENT '用户id',
    name        VARCHAR(50)                 NOT NULL COMMENT '子标签名称',
    create_time datetime DEFAULT NOW()      NULL,
    update_time datetime DEFAULT NOW()      NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='通用子标签表';

CREATE TABLE sync_record
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    user_id     BIGINT                NOT NULL COMMENT '用户id',
    spend_time  DOUBLE(4, 1)          NOT NULL COMMENT '同步耗时',
    sync_count  INT                   NOT NULL COMMENT '本次同步的文件总量',
    create_time datetime              NULL COMMENT '同步时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件同步记录';

CREATE TABLE tag_relation
(
    id          BIGINT UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'ID',
    user_id     BIGINT                         NOT NULL COMMENT '用户ID',
    main_tag_id BIGINT                         NOT NULL COMMENT '主标签ID',
    sub_tag_id  BIGINT                         NOT NULL COMMENT '子标签ID',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='主标签与子标签的关联关系表';

CREATE TABLE user_config
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
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

CREATE TABLE user_info
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

-- 1. 合集主表：存储合集的元数据
CREATE TABLE `analysis_collection`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT,
    `user_id`      bigint       NOT NULL COMMENT '归属用户ID',
    `name`         varchar(128) NOT NULL COMMENT '合集名称',
    `description`  varchar(512) DEFAULT NULL COMMENT '合集描述（可选）',
    `created_time` datetime     DEFAULT CURRENT_TIMESTAMP,
    `updated_time` datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`) -- 常用查询：查某用户的所有合集
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='分析结果合集表';

-- 2. 合集关联表：维护 合集 <-> 分析结果 的多对多关系
CREATE TABLE `collection_relation`
(
    `id`                 bigint NOT NULL AUTO_INCREMENT,
    `collection_id`      bigint NOT NULL COMMENT '合集ID',
    `analysis_result_id` bigint NOT NULL COMMENT '分析结果ID',
    `created_time`       datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_col_res` (`collection_id`, `analysis_result_id`), -- 核心约束：防止同一个结果重复加入同一个合集
    KEY `idx_analysis_result_id` (`analysis_result_id`)              -- 常用查询：查某个结果属于哪些合集
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='合集与分析结果关联表';

-- 3. 测验记录表：存储生成的测验会话
CREATE TABLE `quiz_record`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT,
    `user_id`       bigint       NOT NULL COMMENT '用户ID',
    `collection_id` bigint       NOT NULL COMMENT '来源合集ID',
    `total_score`   int          DEFAULT 0 COMMENT '总分',
    `status`        tinyint      DEFAULT 0 COMMENT '状态 (0=进行中, 1=已完成)',
    `created_time`  datetime     DEFAULT CURRENT_TIMESTAMP,
    `updated_time`  datetime     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_col` (`user_id`, `collection_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI测验会话记录表';

-- 4. 测验题目表：存储具体的题目内容
CREATE TABLE `quiz_question`
(
    `id`                  bigint        NOT NULL AUTO_INCREMENT,
    `quiz_id`             bigint        NOT NULL COMMENT '所属测验ID',
    `related_analysis_id` bigint        DEFAULT NULL COMMENT '关联的原始分析ID (可溯源)',
    `question_text`       text          NOT NULL COMMENT '题干',
    `options_json`        json          NOT NULL COMMENT '选项 (JSON数组)',
    `correct_answer`      varchar(255)  NOT NULL COMMENT '正确答案',
    `explanation`         text          DEFAULT NULL COMMENT '解析',
    `user_answer`         varchar(255)  DEFAULT NULL COMMENT '用户选择的答案',
    `is_correct`          tinyint(1)    DEFAULT NULL COMMENT '是否回答正确',
    `created_time`        datetime      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_quiz_id` (`quiz_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI测验题目表';

create table default_llm_provider
(
    id   int auto_increment
        primary key,
    name varchar(20)  null,
    url  varchar(255) null
)
    comment '默认模型提供商';

create table user_llm_config
(
    id           bigint auto_increment
        primary key,
    user_id      bigint       not null comment '用户id',
    name         varchar(20)  not null,
    url          varchar(255) null,
    api_key      varchar(255) null,
    is_connected tinyint(1)   null comment '是否已连接'
)
    comment '用户模型服务商配置';

create table selected_model
(
    id             int auto_increment
        primary key,
    provider_id    int          not null comment '绑定模型提供商id',
    user_id        bigint       not null comment '用户Id',
    model_name     varchar(100) not null comment '模型名称',
    model_capacity int          null comment '模型能力'
)
    comment '已选择的模型';

-- 成就定义表
create table achievement_definition
(
    id               int auto_increment
        primary key,
    code             varchar(50)  not null unique comment '成就代码',
    name             varchar(100) not null comment '成就名称',
    description      varchar(500) not null comment '成就描述',
    icon             varchar(50)  not null comment '成就图标',
    condition_type   varchar(50)  not null comment '条件类型(count_sync, count_analysis, count_collection, count_quiz, continuous_days)',
    condition_value  int          not null comment '条件值',
    category         varchar(50)  not null comment '成就分类(milestone, activity, knowledge, special)',
    order_index      int          null comment '排序索引',
    created_time     datetime     default current_timestamp null
)
    comment '成就定义表';

-- 用户成就表
create table user_achievement
(
    id               bigint auto_increment
        primary key,
    user_id          bigint       not null comment '用户ID',
    achievement_code varchar(50)  not null comment '成就代码',
    unlocked         tinyint(1)   default 0 null comment '是否解锁',
    progress         int          default 0 null comment '当前进度',
    unlocked_time    datetime     null comment '解锁时间',
    created_time     datetime     default current_timestamp null,
    updated_time     datetime     default current_timestamp on update current_timestamp null,
    constraint uk_user_achievement
        unique (user_id, achievement_code)
)
    comment '用户成就表';

-- 插入成就定义数据
INSERT INTO achievement_definition (code, name, description, icon, condition_type, condition_value, category, order_index) VALUES
-- 里程碑成就 (order_index 1-4)
('first_sync', '初出茅庐', '完成第一次文件同步', 'Document', 'count_sync', 1, 'milestone', 1),
('first_analysis', '初露锋芒', '完成第一次问题分析', 'Edit', 'count_analysis', 1, 'milestone', 2),
('first_collection', '初建知识', '创建第一个知识合集', 'FolderOpened', 'count_collection', 1, 'milestone', 3),
('first_quiz', '小试牛刀', '完成第一次测验', 'CircleCheck', 'count_quiz', 1, 'milestone', 4),

-- 活跃度成就 (order_index 5-10)
('quiz_master_5', '学习达人', '完成5次测验', 'Star', 'count_quiz', 5, 'activity', 5),
('quiz_master_10', '学习专家', '完成10次测验', 'StarFilled', 'count_quiz', 10, 'activity', 6),
('collector_5', '知识收集者', '创建5个合集', 'Folder', 'count_collection', 5, 'activity', 7),
('collector_10', '知识大师', '创建10个合集', 'FolderOpened', 'count_collection', 10, 'activity', 8),
('continuous_7', '坚持不懈', '连续学习7天', 'Calendar', 'continuous_days', 7, 'activity', 9),
('continuous_30', '持之以恒', '连续学习30天', 'CalendarFilled', 'continuous_days', 30, 'activity', 10),

-- 知识掌握成就 (order_index 11-15)
('tag_master_10', '标签专家', '掌握10个知识点', 'PriceTag', 'count_analysis', 10, 'knowledge', 11),
('quiz_perfect_100', '完美主义者', '单次测验满分', 'Trophy', 'count_quiz', 1, 'knowledge', 12),
('java_master', 'Java高手', '掌握10个Java知识点', 'DataLine', 'count_analysis', 10, 'knowledge', 13),
('python_master', 'Python达人', '掌握10个Python知识点', 'ChatDotRound', 'count_analysis', 10, 'knowledge', 14),
('js_master', 'JavaScript专家', '掌握10个JavaScript知识点', 'Coin', 'count_analysis', 10, 'knowledge', 15),

-- 特殊成就 (order_index 16-20)
('continuous_90', '百日铸剑', '连续学习90天', 'Medal', 'continuous_days', 90, 'special', 16),
('sync_master_10', '数据先锋', '同步10次文件', 'Upload', 'count_sync', 10, 'special', 17),
('sync_master_50', '数据巨匠', '同步50次文件', 'UploadFilled', 'count_sync', 50, 'special', 18),
('analysis_master_20', '分析达人', '分析20个问题', 'TrendCharts', 'count_analysis', 20, 'special', 19),
('all_rounder', '全能选手', '解锁所有里程碑成就', 'Crown', 'count_analysis', 4, 'special', 20);

-- 为现有用户初始化成就记录 (假设 user_id = 1 存在)
-- 注意：这需要在 users 表有数据后执行，或者在应用启动时由代码初始化
INSERT INTO user_achievement (user_id, achievement_code, unlocked, progress)
SELECT 8, ad.code, 0, 0
FROM achievement_definition ad;

