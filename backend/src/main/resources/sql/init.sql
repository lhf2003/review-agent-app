create database if not exists review_agent;

use review_agent;

-- 用户信息表
create table if not exists user_info
(
    id          bigint auto_increment primary key comment '用户ID',
    username    varchar(50) comment '用户名',
    password    varchar(255) comment '密码',
    email       varchar(50) comment '邮箱',
    phone       varchar(50) comment '手机号',
    avatar      varchar(255) comment '头像',
    create_time datetime default current_timestamp comment '创建时间',
    update_time datetime default current_timestamp on update current_timestamp comment '更新时间'
);

-- 用户配置表
CREATE TABLE IF NOT EXISTS user_config
(
    id                       INTEGER PRIMARY KEY,
    user_id                  BIGINT NOT NULL comment '用户ID',
    scan_directory           varchar(255) comment '扫描路径',
    auto_scan_enabled        BOOLEAN     DEFAULT TRUE comment '是否自动扫描',
    scan_interval_seconds    INTEGER     DEFAULT 30 comment '扫描间隔（秒，前端以小时选择，范围3600–43200）',
    llm_provider             varchar(50) DEFAULT 'openai' comment 'LLM 提供商',
    openai_api_key_encrypted varchar(255) comment 'API 密钥（加密存储）',
    update_time              datetime    default current_timestamp on update current_timestamp comment '更新时间'
);

-- 文件信息表
CREATE TABLE IF NOT EXISTS data_info
(
    id               BIGINT PRIMARY KEY auto_increment,
    user_id          BIGINT   DEFAULT 1 comment '用户ID',
    file_path        VARCHAR(255) UNIQUE NOT NULL comment '文件路径',
    file_name        VARCHAR(100)        NOT NULL comment '文件名（日期+时间）',
    file_content     TEXT comment '文件内容',
    processed_status TINYINT  DEFAULT 0 comment '处理状态（0=未分析, 1=已分析）',
    created_time     DATETIME DEFAULT CURRENT_TIMESTAMP comment '创建时间'
);

-- AI分析结果表
CREATE TABLE IF NOT EXISTS analysis_result
(
    id                BIGINT PRIMARY KEY auto_increment,
    file_id           INTEGER      NOT NULL comment '文件ID',
    vector_id         VARCHAR(255) NOT NULL comment '向量ID',
    problem_statement VARCHAR(255) comment '问题描述',
    solution          TEXT comment '解决方案',
    status            TINYINT  DEFAULT 0 comment '处理状态（0=失败, 1=成功）',
    created_time      datetime default current_timestamp comment '创建时间'
);

-- 索引：按文件查询分析结果
CREATE INDEX idx_analysis_result_file_id ON analysis_result (file_id);

-- 分析结果标签关联表
CREATE TABLE IF NOT EXISTS analysis_tag
(
    id               bigint auto_increment
        primary key,
    analysis_id      BIGINT  NOT NULL comment '分析结果ID',
    tag_relation_id           INTEGER NOT NULL comment '标签关联ID',
    confidence_score REAL comment 'AI 判断置信度'
);

-- 文件同步记录表
CREATE TABLE IF NOT EXISTS sync_record
(
    id          bigint auto_increment
        primary key,
    user_id     bigint       not null comment '用户id',
    spend_time  double(4, 1) not null comment '同步耗时',
    sync_count  int          not null comment '本次同步的文件总量',
    create_time datetime     null comment '同步时间'
)
    comment '文件同步记录';


-- 顶层标签
CREATE TABLE main_tag
(
    id         INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '主标签ID',
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    name       VARCHAR(50) NOT NULL UNIQUE COMMENT '主标签名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT ='主分类标签表';

-- 子标签
CREATE TABLE sub_tag
(
    id         INT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '子标签ID',
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    name       VARCHAR(50) NOT NULL UNIQUE COMMENT '子标签名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT ='通用子标签表';


-- 关联主标签和子标签
CREATE TABLE tag_relation
(
    id          BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    main_tag_id BIGINT NOT NULL COMMENT '主标签ID',
    sub_tag_id  BIGINT NOT NULL COMMENT '子标签ID',
    PRIMARY KEY (main_tag_id, sub_tag_id) COMMENT '联合主键，确保每对关系唯一'

) COMMENT ='主标签与子标签的关联关系表';



