-- ================================================
-- Review Agent 练习题功能增强数据库迁移脚本
--
-- 说明：
--   1. 扩展 quiz_question 表，支持多种题型、难度等级、知识点标记
--   2. 新增 quiz_mistake 表，实现错题本功能
--   3. 新增 knowledge_mastery 表，实现知识点掌握度追踪
--   4. 添加相关索引优化查询性能
--
-- 执行方式：
--   mysql -u root -p review_agent < backend/src/main/resources/sql/migration_quiz_enhancements.sql
--
-- 注意事项：
--   1. 执行前请备份数据库
--   2. 现有 quiz_question 记录的 question_type 将默认设为 'single_choice'
--   3. 难度等级默认为 3（中等）
--   4. 答题时限默认为 60 秒
--
-- 作者：Review Agent Team
-- 日期：2026-02-01
-- 版本：v1.0
-- ================================================

-- ================================================
-- Part 1: 扩展 quiz_question 表
-- ================================================

-- 1.1 添加题目类型字段
ALTER TABLE quiz_question
ADD COLUMN question_type VARCHAR(20) NOT NULL DEFAULT 'single_choice'
    COMMENT '题目类型 (single_choice-单选题, multiple_choice-多选题, true_false-判断题, fill_blank-填空题, code_snippet-代码识别题)';

-- 1.2 添加难度等级字段
ALTER TABLE quiz_question
ADD COLUMN difficulty_level TINYINT DEFAULT 3 COMMENT '难度等级 (1-非常简单, 2-简单, 3-中等, 4-困难, 5-非常困难)';

-- 1.3 添加知识点标签字段
ALTER TABLE quiz_question
ADD COLUMN knowledge_point VARCHAR(100) COMMENT '知识点标签（用于掌握度分析）';

-- 1.4 添加答题时限字段
ALTER TABLE quiz_question
ADD COLUMN time_limit INT DEFAULT 60 COMMENT '答题时限（秒）';

-- 1.5 添加统计字段
ALTER TABLE quiz_question
ADD COLUMN answer_count INT DEFAULT 0 COMMENT '被回答次数（用于题目质量评估）',
ADD COLUMN correct_count INT DEFAULT 0 COMMENT '正确次数（用于难度校准）';

-- 1.6 为现有数据设置默认值
UPDATE quiz_question
SET question_type = 'single_choice',
    difficulty_level = 3,
    time_limit = 60,
    answer_count = 0,
    correct_count = 0
WHERE question_type IS NULL;

-- ================================================
-- Part 2: 新增 quiz_mistake 表（错题本）
-- ================================================

CREATE TABLE IF NOT EXISTS `quiz_mistake` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `question_id` BIGINT NOT NULL COMMENT '题目ID',
    `quiz_id` BIGINT COMMENT '来源测验ID（可溯源）',
    `mistake_count` INT DEFAULT 1 COMMENT '错误次数',
    `last_mistake_time` DATETIME COMMENT '最后一次错误时间',
    `mastered` BOOLEAN DEFAULT FALSE COMMENT '是否已掌握（连续答对3次标记为掌握）',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_mastered` (`user_id`, `mastered`),
    INDEX `idx_question` (`question_id`),
    INDEX `idx_last_mistake` (`user_id`, `last_mistake_time`),
    CONSTRAINT `fk_mistake_user`
        FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mistake_question`
        FOREIGN KEY (`question_id`) REFERENCES `quiz_question` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mistake_quiz`
        FOREIGN KEY (`quiz_id`) REFERENCES `quiz_record` (`id`) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '错题本表（记录用户答错的题目及掌握状态）';

-- ================================================
-- Part 3: 新增 knowledge_mastery 表（知识点掌握度）
-- ================================================

CREATE TABLE IF NOT EXISTS `knowledge_mastery` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `knowledge_point` VARCHAR(100) NOT NULL COMMENT '知识点',
    `total_answered` INT DEFAULT 0 COMMENT '总答题次数',
    `correct_count` INT DEFAULT 0 COMMENT '正确次数',
    `mastery_score` DECIMAL(5,2) DEFAULT 0 COMMENT '掌握度 (0-100)',
    `average_time` INT COMMENT '平均答题时间（秒）',
    `last_practice_time` DATETIME COMMENT '最后一次练习时间',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_knowledge` (`user_id`, `knowledge_point`),
    INDEX `idx_mastery_score` (`user_id`, `mastery_score`),
    INDEX `idx_last_practice` (`user_id`, `last_practice_time`),
    CONSTRAINT `fk_mastery_user`
        FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '知识点掌握度表（追踪用户对各知识点的掌握程度）';

-- ================================================
-- Part 4: 添加新索引优化性能
-- ================================================

-- 4.1 quiz_question 表的新索引
CREATE INDEX idx_quiz_question_type ON quiz_question(question_type);
CREATE INDEX idx_quiz_question_difficulty ON quiz_question(difficulty_level);
CREATE INDEX idx_quiz_question_knowledge ON quiz_question(knowledge_point);

-- 4.2 quiz_question 表的复合索引（用于查询优化）
CREATE INDEX idx_quiz_quiz_difficulty ON quiz_question(quiz_id, difficulty_level);
CREATE INDEX idx_quiz_user_knowledge ON quiz_question(knowledge_point, difficulty_level);

-- ================================================
-- Part 5: 数据完整性检查（可选，执行后可删除）
-- ================================================

-- 检查是否有知识点未提取的题目
-- SELECT COUNT(*) as orphan_questions FROM quiz_question WHERE knowledge_point IS NULL;

-- 检查现有错题记录的统计
-- SELECT
--     q.id,
--     q.question_text,
--     m.mistake_count,
--     m.mastered
-- FROM quiz_mistake m
-- JOIN quiz_question q ON m.question_id = q.id
-- ORDER BY m.mistake_count DESC;

-- ================================================
-- 迁移完成
-- ================================================
