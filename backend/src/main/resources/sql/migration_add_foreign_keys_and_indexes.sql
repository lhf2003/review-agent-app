-- ================================================
-- Review Agent 数据库优化迁移脚本
--
-- 说明：
--   1. 添加外键约束，保证数据一致性
--   2. 添加复合索引，提升查询性能
--   3. 添加单列索引，加速常用查询
--
-- 执行方式：
--   mysql -u root -p review_agent < backend/src/main/resources/sql/migration_add_foreign_keys_and_indexes.sql
--
-- 注意事项：
--   1. 执行前请备份数据库
--   2. 如果外键约束添加失败，可能是数据不一致，需要先清理脏数据
--   3. 建议在低峰期执行，避免影响线上服务
--
-- 作者：Review Agent Team
-- 日期：2026-01-31
-- ================================================

-- ================================================
-- Part 1: 外键约束
-- ================================================

-- 1.1 data_info 表的外键约束
-- data_info.user_id → user_info.id (级联删除)
ALTER TABLE data_info
ADD CONSTRAINT fk_data_info_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- 1.2 analysis_result 表的外键约束
-- analysis_result.user_id → user_info.id (级联删除)
ALTER TABLE analysis_result
ADD CONSTRAINT fk_analysis_result_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- analysis_result.file_id → data_info.id (级联删除)
ALTER TABLE analysis_result
ADD CONSTRAINT fk_analysis_result_data
FOREIGN KEY (file_id) REFERENCES data_info(id) ON DELETE CASCADE;

-- 1.3 analysis_tag 表的外键约束
-- analysis_tag.analysis_id → analysis_result.id (级联删除)
ALTER TABLE analysis_tag
ADD CONSTRAINT fk_analysis_tag_analysis
FOREIGN KEY (analysis_id) REFERENCES analysis_result(id) ON DELETE CASCADE;

-- 1.4 main_tag 表的外键约束
-- main_tag.user_id → user_info.id (级联删除)
ALTER TABLE main_tag
ADD CONSTRAINT fk_main_tag_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- 1.5 sub_tag 表的外键约束
-- sub_tag.user_id → user_info.id (级联删除)
ALTER TABLE sub_tag
ADD CONSTRAINT fk_sub_tag_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- 1.6 tag_relation 表的外键约束
-- tag_relation.user_id → user_info.id (级联删除)
ALTER TABLE tag_relation
ADD CONSTRAINT fk_tag_relation_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- tag_relation.main_tag_id → main_tag.id (级联删除)
ALTER TABLE tag_relation
ADD CONSTRAINT fk_tag_relation_main_tag
FOREIGN KEY (main_tag_id) REFERENCES main_tag(id) ON DELETE CASCADE;

-- tag_relation.sub_tag_id → sub_tag.id (级联删除)
ALTER TABLE tag_relation
ADD CONSTRAINT fk_tag_relation_sub_tag
FOREIGN KEY (sub_tag_id) REFERENCES sub_tag(id) ON DELETE CASCADE;

-- 1.7 user_config 表的外键约束
-- user_config.user_id → user_info.id (级联删除)
ALTER TABLE user_config
ADD CONSTRAINT fk_user_config_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- 1.8 sync_record 表的外键约束
-- sync_record.user_id → user_info.id (级联删除)
ALTER TABLE sync_record
ADD CONSTRAINT fk_sync_record_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- 1.9 report_data 表的外键约束
-- report_data.user_id → user_info.id (级联删除)
ALTER TABLE report_data
ADD CONSTRAINT fk_report_data_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- 1.10 analysis_collection 表的外键约束
-- 注意：analysis_collection 已有 idx_user_id 索引，添加外键会自动利用该索引
-- analysis_collection.user_id → user_info.id (级联删除)
ALTER TABLE analysis_collection
ADD CONSTRAINT fk_analysis_collection_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- 1.11 collection_relation 表的外键约束
-- collection_relation.collection_id → analysis_collection.id (级联删除)
ALTER TABLE collection_relation
ADD CONSTRAINT fk_collection_relation_collection
FOREIGN KEY (collection_id) REFERENCES analysis_collection(id) ON DELETE CASCADE;

-- 1.12 quiz_record 表的外键约束
-- quiz_record.user_id → user_info.id (级联删除)
ALTER TABLE quiz_record
ADD CONSTRAINT fk_quiz_record_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- quiz_record.collection_id → analysis_collection.id (级联删除)
ALTER TABLE quiz_record
ADD CONSTRAINT fk_quiz_record_collection
FOREIGN KEY (collection_id) REFERENCES analysis_collection(id) ON DELETE CASCADE;

-- quiz_question.quiz_id → quiz_record.id (级联删除)
ALTER TABLE quiz_question
ADD CONSTRAINT fk_quiz_question_record
FOREIGN KEY (quiz_id) REFERENCES quiz_record(id) ON DELETE CASCADE;

-- 1.13 user_achievement 表的外键约束
-- user_achievement.user_id → user_info.id (级联删除)
ALTER TABLE user_achievement
ADD CONSTRAINT fk_user_achievement_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- 1.14 user_llm_config 表的外键约束
-- user_llm_config.user_id → user_info.id (级联删除)
ALTER TABLE user_llm_config
ADD CONSTRAINT fk_user_llm_config_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- 1.15 selected_model 表的外键约束
-- selected_model.user_id → user_info.id (级联删除)
ALTER TABLE selected_model
ADD CONSTRAINT fk_selected_model_user
FOREIGN KEY (user_id) REFERENCES user_info(id) ON DELETE CASCADE;

-- 1.16 llm_provider 表（无需外键，是独立的配置表）

-- 1.17 default_llm_provider 表（无需外键，是独立的配置表）

-- 1.18 achievement_definition 表（无需外键，是全局配置表）

-- ================================================
-- Part 2: 复合索引
-- ================================================

-- 2.1 data_info 表的复合索引
-- 用于：查询某个用户的文件列表，按时间倒序
CREATE INDEX idx_data_info_user_created
ON data_info(user_id, created_time DESC);

-- 2.2 analysis_result 表的复合索引
-- 用于：查询某个用户的所有分析结果，按时间倒序
CREATE INDEX idx_analysis_result_user_created
ON analysis_result(user_id, created_time DESC);

-- 用于：查询某个文件的所有分析结果，按状态过滤
CREATE INDEX idx_analysis_result_file_status
ON analysis_result(file_id, status);

-- 2.3 analysis_tag 表的复合索引
-- 用于：查询某个分析结果的所有标签
CREATE INDEX idx_analysis_tag_analysis_id
ON analysis_tag(analysis_id);

-- 用于：按标签 ID 查询分析结果
CREATE INDEX idx_analysis_tag_tag_id
ON analysis_tag(tag_id);

-- 2.4 analysis_collection 表的复合索引
-- 用于：查询某个用户的合集，按更新时间倒序
CREATE INDEX idx_analysis_collection_user_updated
ON analysis_collection(user_id, updated_time DESC);

-- 2.5 collection_relation 表的复合索引
-- 用于：查询某个合集的所有关联条目
CREATE INDEX idx_collection_relation_collection_created
ON collection_relation(collection_id, created_time DESC);

-- 2.6 quiz_record 表的复合索引
-- 用于：查询某个用户的所有测验记录
CREATE INDEX idx_quiz_record_user_created
ON quiz_record(user_id, created_time DESC);

-- 用于：查询某个合集的所有测验记录
CREATE INDEX idx_quiz_record_collection_created
ON quiz_record(collection_id, created_time DESC);

-- 用于：查询某个用户在某个合集的测验记录
CREATE INDEX idx_quiz_record_user_collection
ON quiz_record(user_id, collection_id, created_time DESC);

-- 2.7 quiz_question 表的复合索引
-- 用于：查询某个测验的所有题目
CREATE INDEX idx_quiz_question_quiz_id
ON quiz_question(quiz_id);

-- 2.8 tag_relation 表的复合索引
-- 用于：查询某个主标签的所有子标签
CREATE INDEX idx_tag_relation_main_tag
ON tag_relation(main_tag_id);

-- 用于：查询某个子标签的所有主标签
CREATE INDEX idx_tag_relation_sub_tag
ON tag_relation(sub_tag_id);

-- 2.9 sync_record 表的复合索引
-- 用于：查询某个用户的同步历史，按时间倒序
CREATE INDEX idx_sync_record_user_created
ON sync_record(user_id, create_time DESC);

-- 2.10 report_data 表的复合索引
-- 用于：查询某个用户的日报/周报
CREATE INDEX idx_report_data_user_type
ON report_data(user_id, type, start_date DESC);

-- ================================================
-- Part 3: 单列索引（补充）
-- ================================================

-- 3.1 user_info 表的单列索引
-- 用户名索引（用于登录查询）
CREATE INDEX idx_user_username
ON user_info(username);

-- 邮箱索引（用于找回密码）
CREATE INDEX idx_user_email
ON user_info(email);

-- 3.2 data_info 表的单列索引
-- 文件名索引（用于文件搜索）
CREATE INDEX idx_data_info_file_name
ON data_info(file_name);

-- 处理状态索引（用于筛选未处理/已处理的文件）
CREATE INDEX idx_data_info_processed_status
ON data_info(processed_status);

-- 数据来源索引（用于筛选本地/Gemini/ChatGPT 数据）
CREATE INDEX idx_data_info_source
ON data_info(source);

-- 3.3 analysis_result 表的单列索引
-- 问题陈述索引（用于全文搜索）
CREATE INDEX idx_analysis_result_problem_statement
ON analysis_result(problem_statement(255));

-- 创建时间索引（单独索引，用于时间范围查询）
CREATE INDEX idx_analysis_result_created_time
ON analysis_result(created_time);

-- 3.4 main_tag 表的单列索引
-- 标签名称索引（用于标签搜索）
CREATE INDEX idx_main_tag_name
ON main_tag(name);

-- 创建时间索引
CREATE INDEX idx_main_tag_created_time
ON main_tag(create_time);

-- 3.5 sub_tag 表的单列索引
-- 子标签名称索引（用于子标签搜索）
CREATE INDEX idx_sub_tag_name
ON sub_tag(name);

-- 3.6 quiz_question 表的单列索引
-- 关联分析结果 ID 索引（用于追溯）
CREATE INDEX idx_quiz_question_related_analysis_id
ON quiz_question(related_analysis_id);

-- 3.7 user_achievement 表的单列索引
-- 成就代码索引（用于查询某个成就的所有用户）
CREATE INDEX idx_user_achievement_code
ON user_achievement(achievement_code);

-- 解锁状态索引（用于查询已解锁的成就）
CREATE INDEX idx_user_achievement_unlocked
ON user_achievement(unlocked);

-- 解锁时间索引（用于按时间排序）
CREATE INDEX idx_user_achievement_unlocked_time
ON user_achievement(unlocked_time);

-- ================================================
-- Part 4: 验证查询（执行后可用于验证索引是否生效）
-- ================================================

-- 验证 data_info 的索引
-- EXPLAIN SELECT * FROM data_info WHERE user_id = 1 ORDER BY created_time DESC LIMIT 10;

-- 验证 analysis_result 的索引
-- EXPLAIN SELECT * FROM analysis_result WHERE user_id = 1 ORDER BY created_time DESC LIMIT 10;

-- 验证 quiz_record 的索引
-- EXPLAIN SELECT * FROM quiz_record WHERE user_id = 1 AND collection_id = 1 ORDER BY created_time DESC LIMIT 10;

-- 验证 tag_relation 的索引
-- EXPLAIN SELECT * FROM tag_relation WHERE main_tag_id = 1;

-- ================================================
-- 完成提示
-- ================================================
-- 执行完成后，可以通过以下方式验证：
-- 1. SHOW CREATE TABLE table_name; -- 查看表结构和约束
-- 2. SHOW INDEX FROM table_name; -- 查看表的索引列表
-- 3. SELECT * FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = 'review_agent'; -- 查看所有外键
