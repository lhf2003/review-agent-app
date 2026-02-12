-- ============================================
-- 答题系统版本追踪 - 数据库迁移脚本
-- 创建日期: 2025-02-03
-- 版本: v1.0
-- ============================================

-- 添加版本追踪字段到 quiz_record 表
ALTER TABLE quiz_record
ADD COLUMN collection_version_hash VARCHAR(64) COMMENT '合集内容哈希值（用于版本检测）',
ADD COLUMN analysis_result_ids TEXT COMMENT '生成题库时使用的分析结果ID列表（JSON格式）',
ADD COLUMN is_outdated BOOLEAN DEFAULT FALSE COMMENT '题型是否已过期（合集有新内容）';

-- 创建索引以优化版本查询性能
CREATE INDEX idx_quiz_collection_version ON quiz_record(collection_id, is_outdated);

-- 为现有数据生成版本哈希
UPDATE quiz_record qr
SET collection_version_hash = (
    SELECT MD5(
        GROUP_CONCAT(
            CAST(cr.analysis_result_id AS CHAR)
            ORDER BY cr.analysis_result_id
        )
    )
    FROM collection_relation cr
    WHERE cr.collection_id = qr.collection_id
)
WHERE collection_version_hash IS NULL;

-- 为现有数据填充 analysis_result_ids
UPDATE quiz_record qr
SET analysis_result_ids = (
    SELECT CONCAT('[',
        GROUP_CONCAT(
            JSON_QUOTE(CAST(cr.analysis_result_id AS CHAR))
            ORDER BY cr.analysis_result_id
        ),
    ']')
    FROM collection_relation cr
    WHERE cr.collection_id = qr.collection_id
)
WHERE analysis_result_ids IS NULL;

-- 确保所有现有记录的 is_outdated 默认为 FALSE
UPDATE quiz_record SET is_outdated = FALSE WHERE is_outdated IS NULL;
