-- 通知设置表
-- 用于存储用户的复习提醒偏好设置
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
