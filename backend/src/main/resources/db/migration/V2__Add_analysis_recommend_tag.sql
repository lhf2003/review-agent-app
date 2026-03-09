-- ================================================
-- 推荐标签功能支持
-- Flyway Version: V2
-- ================================================

-- 推荐标签关联表
-- 存储AI分析时LLM建议的新标签，支持用户采纳或忽略
CREATE TABLE IF NOT EXISTS analysis_recommend_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    analysis_result_id BIGINT NOT NULL COMMENT '关联的分析结果ID',
    tag_name VARCHAR(100) NOT NULL COMMENT '推荐标签名称',
    user_action VARCHAR(20) DEFAULT 'PENDING' COMMENT '用户操作状态：PENDING(待处理)/ADOPTED(已采纳)/IGNORED(已忽略)',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 唯一约束：同一分析结果的同一推荐标签只记录一次
    UNIQUE KEY uk_analysis_tag_name (analysis_result_id, tag_name),

    -- 索引
    INDEX idx_user_action (user_id, user_action) COMMENT '按用户和操作状态查询',
    INDEX idx_analysis_result (analysis_result_id) COMMENT '按分析结果查询',
    INDEX idx_created_time (created_time) COMMENT '按创建时间排序',

    -- 外键约束
    FOREIGN KEY (analysis_result_id) REFERENCES analysis_result(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析结果推荐标签表';

-- 添加分析结果表的推荐标签统计字段（可选，用于快速查询）
-- 注意：这个字段是冗余的，主要用于列表展示时避免关联查询
ALTER TABLE analysis_result
    ADD COLUMN recommend_tag_count INT DEFAULT 0 COMMENT '推荐标签数量（冗余字段）',
    ADD COLUMN adopted_tag_count INT DEFAULT 0 COMMENT '已采纳标签数量（冗余字段）';

-- 创建触发器：自动更新推荐标签数量统计
-- 注：如果MySQL版本支持，可以使用以下触发器；否则需要在应用层维护
DELIMITER //

CREATE TRIGGER trg_recommend_tag_insert
AFTER INSERT ON analysis_recommend_tag
FOR EACH ROW
BEGIN
    UPDATE analysis_result
    SET recommend_tag_count = (
        SELECT COUNT(*) FROM analysis_recommend_tag WHERE analysis_result_id = NEW.analysis_result_id
    ),
    adopted_tag_count = (
        SELECT COUNT(*) FROM analysis_recommend_tag
        WHERE analysis_result_id = NEW.analysis_result_id AND user_action = 'ADOPTED'
    )
    WHERE id = NEW.analysis_result_id;
END//

CREATE TRIGGER trg_recommend_tag_update
AFTER UPDATE ON analysis_recommend_tag
FOR EACH ROW
BEGIN
    IF NEW.user_action != OLD.user_action THEN
        UPDATE analysis_result
        SET adopted_tag_count = (
            SELECT COUNT(*) FROM analysis_recommend_tag
            WHERE analysis_result_id = NEW.analysis_result_id AND user_action = 'ADOPTED'
        )
        WHERE id = NEW.analysis_result_id;
    END IF;
END//

CREATE TRIGGER trg_recommend_tag_delete
AFTER DELETE ON analysis_recommend_tag
FOR EACH ROW
BEGIN
    UPDATE analysis_result
    SET recommend_tag_count = (
        SELECT COUNT(*) FROM analysis_recommend_tag WHERE analysis_result_id = OLD.analysis_result_id
    ),
    adopted_tag_count = (
        SELECT COUNT(*) FROM analysis_recommend_tag
        WHERE analysis_result_id = OLD.analysis_result_id AND user_action = 'ADOPTED'
    )
    WHERE id = OLD.analysis_result_id;
END//

DELIMITER ;
