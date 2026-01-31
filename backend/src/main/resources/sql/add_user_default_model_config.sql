-- 用户默认模型配置表
-- 用于存储用户为不同功能场景配置的默认模型

CREATE TABLE `user_default_model_config`
(
    `id`          BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键ID',
    `user_id`     BIGINT               NOT NULL COMMENT '用户ID',
    `model_type`   VARCHAR(50)          NOT NULL COMMENT '模型类型：SESSION_SPLIT(会话拆分), TAG_CLASSIFICATION(标签分类), SMART_ANALYSIS(智能分析)',
    `provider_id`  INT                  NULL COMMENT '模型提供商ID（关联 user_llm_config 表）',
    `model_name`   VARCHAR(100)         NOT NULL COMMENT '选中的模型名称',
    `created_time` DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_model_type` (`user_id`, `model_type`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_provider_id` (`provider_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户默认模型配置表';
