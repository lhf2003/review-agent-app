CREATE TABLE analysis_result
(
    id                BIGINT AUTO_INCREMENT  NOT NULL,
    file_id           BIGINT  NOT NULL COMMENT '文件数据ID',
    vector_id         VARCHAR(255) NULL COMMENT '向量ID',
    user_id           BIGINT  NOT NULL COMMENT '用户id',
    problem_statement VARCHAR(255) NULL COMMENT '用户的问题描述',
    solution          LONGTEXT NULL COMMENT 'AI回复的解决方案',
    session_start     TINYINT NULL COMMENT '会话开始索引',
    session_end       TINYINT NULL COMMENT '会话结束索引',
    session_content   LONGTEXT NULL COMMENT '会话内容',
    status            TINYINT NOT NULL COMMENT '状态（0=失败 1=成功）',
    created_time      datetime DEFAULT NOW() NULL COMMENT '创建时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

CREATE TABLE analysis_tag
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    analysis_id BIGINT NOT NULL COMMENT '分析结果ID',
    tag_id      INT NULL COMMENT '标签ID',
    sub_tag_id  VARCHAR(100) NULL COMMENT '子标签Id列表',
    recommends  VARCHAR(100) NULL COMMENT 'AI推荐的主标签',
    confidence_score DOUBLE NULL COMMENT 'AI 判断置信度',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

CREATE TABLE data_info
(
    id               BIGINT AUTO_INCREMENT  NOT NULL,
    user_id          BIGINT   DEFAULT 1 NULL COMMENT '用户ID',
    file_path        VARCHAR(255) NOT NULL COMMENT '文件路径',
    file_name        VARCHAR(100) NOT NULL COMMENT '文件名（日期+时间）',
    file_content     LONGTEXT NULL COMMENT '文件内容',
    processed_status TINYINT  DEFAULT 0 NULL COMMENT '处理状态（0=未分析, 1=正在分析 2=已分析 3=有更新 4=分析失败）',
    created_time     datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time      datetime NULL COMMENT '文件上一次修改时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件信息（模型对话数据）';

CREATE TABLE llm_provider
(
    id          INT AUTO_INCREMENT NOT NULL,
    name        VARCHAR(20) NULL COMMENT '提供商名称',
    request_url VARCHAR(200) NULL COMMENT '请求地址',
    api_key     VARCHAR(255) NOT NULL COMMENT 'APIKEY',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='模型提供商';

CREATE TABLE main_tag
(
    id          INT UNSIGNED AUTO_INCREMENT NOT NULL COMMENT '主标签ID',
    user_id     BIGINT      NOT NULL COMMENT '用户id',
    name        VARCHAR(50) NOT NULL COMMENT '主标签名称',
    create_time datetime DEFAULT NOW() NULL,
    update_time datetime DEFAULT NOW() NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='主分类标签表';

CREATE TABLE report_data
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    user_id        BIGINT   NOT NULL,
    report_content LONGTEXT NOT NULL COMMENT '报告内容',
    type           TINYINT NULL COMMENT '报告类型（1日报 2周报）',
    start_date     date NULL COMMENT '开始时间',
    end_date       date NULL COMMENT '结束时间',
    create_time    datetime NULL COMMENT '生成时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='日/周报记录';

CREATE TABLE sub_tag
(
    id          INT UNSIGNED AUTO_INCREMENT NOT NULL COMMENT '子标签ID',
    user_id     BIGINT      NOT NULL COMMENT '用户id',
    name        VARCHAR(50) NOT NULL COMMENT '子标签名称',
    create_time datetime DEFAULT NOW() NULL,
    update_time datetime DEFAULT NOW() NULL,
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='通用子标签表';

CREATE TABLE sync_record
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    user_id     BIGINT NOT NULL COMMENT '用户id',
    spend_time DOUBLE(4, 1)          NOT NULL COMMENT '同步耗时',
    sync_count  INT    NOT NULL COMMENT '本次同步的文件总量',
    create_time datetime NULL COMMENT '同步时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='文件同步记录';

CREATE TABLE tag_relation
(
    id          BIGINT UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    main_tag_id BIGINT NOT NULL COMMENT '主标签ID',
    sub_tag_id  BIGINT NOT NULL COMMENT '子标签ID',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
) COMMENT ='主标签与子标签的关联关系表';

CREATE TABLE user_config
(
    id                       BIGINT AUTO_INCREMENT        NOT NULL,
    user_id                  BIGINT NOT NULL COMMENT '关联用户id',
    scan_directory           VARCHAR(255) NULL COMMENT '扫描路径',
    auto_scan_enabled        TINYINT(1)  DEFAULT 1        NULL COMMENT '是否自动扫描',
    scan_interval_seconds    INT         DEFAULT 30 NULL COMMENT '扫描间隔（秒）',
    llm_provider             VARCHAR(50) DEFAULT 'openai' NULL COMMENT 'LLM 提供商',
    tag_llm_provider         INT NULL,
    openai_api_key_encrypted VARCHAR(255) NULL COMMENT 'API 密钥（加密存储）',
    update_time              timestamp   DEFAULT NOW() NULL COMMENT '更新时间',
    session_llm_provider     INT NULL,
    daily_enabled            TINYINT NULL COMMENT '是否启用日报功能',
    daily_cron               VARCHAR(50) NULL COMMENT '指定日报的cron',
    weekly_enabled           TINYINT NULL COMMENT '是否启用周报功能',
    weekly_cron              VARCHAR(50) NULL COMMENT '指定周报的cron',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

CREATE TABLE user_info
(
    id          BIGINT AUTO_INCREMENT  NOT NULL COMMENT '用户ID',
    username    VARCHAR(50) NULL COMMENT '用户名',
    password    VARCHAR(255) NULL COMMENT '密码',
    email       VARCHAR(50) NULL COMMENT '邮箱',
    phone       VARCHAR(50) NULL COMMENT '手机号',
    avatar      VARCHAR(255) NULL COMMENT '头像',
    create_time datetime DEFAULT NOW() NULL COMMENT '创建时间',
    update_time datetime DEFAULT NOW() NULL COMMENT '更新时间',
    CONSTRAINT `PRIMARY` PRIMARY KEY (id)
);

ALTER TABLE data_info
    ADD CONSTRAINT file_path UNIQUE (file_path);

ALTER TABLE main_tag
    ADD CONSTRAINT name UNIQUE (name);

ALTER TABLE sub_tag
    ADD CONSTRAINT name UNIQUE (name);
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
    file_path        VARCHAR(255)           NOT NULL COMMENT '文件路径',
    file_name        VARCHAR(100)           NOT NULL COMMENT '文件名（日期+时间）',
    file_content     LONGTEXT               NULL COMMENT '文件内容',
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

ALTER TABLE data_info
    ADD CONSTRAINT file_path UNIQUE (file_path);

ALTER TABLE main_tag
    ADD CONSTRAINT name UNIQUE (name);

ALTER TABLE sub_tag
    ADD CONSTRAINT name UNIQUE (name);