-- ================================================
-- Review Agent 软删除机制数据库迁移脚本
--
-- 说明：
--   1. 为需要的表添加 deleted 和 deleted_at 字段
--   2. 为现有数据设置默认值（deleted = false, deleted_at = NULL）
--   3. 添加索引以优化软删除查询性能
--
-- 执行方式：
--   mysql -u root -p review_agent < backend/src/main/resources/sql/migration_soft_delete.sql
--
-- 注意事项：
--   1. 执行前请备份数据库
--   2. 删除后的数据可以恢复（设置 deleted = false）
--   3. 物理删除需要使用 Repository 中的 hardDelete 方法
--
-- 作者：Review Agent Team
-- 日期：2026-01-31
-- ================================================

-- ================================================
-- Part 1: 添加软删除字段
-- ================================================

-- 1.1 data_info 表
ALTER TABLE data_info
ADD COLUMN deleted TINYINT(1) DEFAULT 0 NOT NULL COMMENT '删除标记（0-未删除 1-已删除）',
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '删除时间';

-- 1.2 analysis_result 表
ALTER TABLE analysis_result
ADD COLUMN deleted TINYINT(1) DEFAULT 0 NOT NULL COMMENT '删除标记（0-未删除 1-已删除）',
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '删除时间';

-- 1.3 analysis_collection 表
ALTER TABLE analysis_collection
ADD COLUMN deleted TINYINT(1) DEFAULT 0 NOT NULL COMMENT '删除标记（0-未删除 1-已删除）',
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '删除时间';

-- 1.4 quiz_record 表
ALTER TABLE quiz_record
ADD COLUMN deleted TINYINT(1) DEFAULT 0 NOT NULL COMMENT '删除标记（0-未删除 1-已删除）',
ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '删除时间';

-- ================================================
-- Part 2: 添加软删除索引
-- ================================================

-- 2.1 data_info 表的软删除索引
CREATE INDEX idx_data_info_deleted
ON data_info(deleted);

CREATE INDEX idx_data_info_user_deleted
ON data_info(user_id, deleted);

-- 2.2 analysis_result 表的软删除索引
CREATE INDEX idx_analysis_result_deleted
ON analysis_result(deleted);

CREATE INDEX idx_analysis_result_user_deleted
ON analysis_result(user_id, deleted);

-- 2.3 analysis_collection 表的软删除索引
CREATE INDEX idx_analysis_collection_deleted
ON analysis_collection(deleted);

CREATE INDEX idx_analysis_collection_user_deleted
ON analysis_collection(user_id, deleted);

-- 2.4 quiz_record 表的软删除索引
CREATE INDEX idx_quiz_record_deleted
ON quiz_record(deleted);

CREATE INDEX idx_quiz_record_user_deleted
ON quiz_record(user_id, deleted);

CREATE INDEX idx_quiz_record_collection_deleted
ON quiz_record(collection_id, deleted);

-- ================================================
-- Part 3: 验证查询（执行后可用于验证字段是否生效）
-- ================================================

-- 验证 data_info 的软删除字段
-- DESCRIBE data_info;

-- 验证 analysis_result 的软删除字段
-- DESCRIBE analysis_result;

-- 验证软删除索引
-- SHOW INDEX FROM data_info WHERE Key_name LIKE '%deleted%';
-- SHOW INDEX FROM analysis_result WHERE Key_name LIKE '%deleted%';

-- 测试软删除查询（只查询未删除的）
-- SELECT COUNT(*) FROM data_info WHERE deleted = 0;
-- SELECT COUNT(*) FROM data_info WHERE deleted = 1;

-- ================================================
-- 软删除使用说明
-- ================================================
--
-- 1. 软删除：
--    Repository 方法：softDelete(Long id, Date deletedAt)
--    Service 方法调用：dataInfoRepository.softDelete(id, new Date())
--
-- 2. 恢复删除的数据：
--    Service 方法调用：
--    dataInfo.setDeleted(false);
--    dataInfo.setDeletedAt(null);
--    dataInfoRepository.save(dataInfo);
--
-- 3. 物理删除（慎用）：
--    Repository 方法：hardDelete(Long id)
--    注意：物理删除会级联删除关联数据（如果有外键）
--
-- 4. 查询时自动过滤已删除数据：
--    JPA Query 中添加：and deleted = false
--    例如：select d from DataInfo d where d.userId = :userId and d.deleted = false
--
-- 5. 统计未删除的数据：
--    @Query("select count(d) from DataInfo d where d.userId = :userId and d.deleted = false")
--    long countByUserId(Long userId);
--
-- ================================================
-- 完成提示
-- ================================================
-- 执行完成后，可以通过以下方式验证：
-- 1. SHOW COLUMNS FROM table_name LIKE '%deleted%'; -- 查看表的软删除字段
-- 2. SHOW INDEX FROM table_name; -- 查看表的索引列表
-- 3. SELECT COUNT(*) FROM table_name WHERE deleted = 0; -- 统计未删除数据
-- 4. SELECT COUNT(*) FROM table_name WHERE deleted = 1; -- 统计已删除数据
