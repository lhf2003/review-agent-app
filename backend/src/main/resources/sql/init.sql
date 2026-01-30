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

