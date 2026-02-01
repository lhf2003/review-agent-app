-- 修复 quiz_question 表中的 question_type 字段
-- 将枚举名称（SINGLE_CHOICE）转换为 code 值（single_choice）

UPDATE review_agent.quiz_question
SET question_type = CASE
    WHEN question_type = 'SINGLE_CHOICE' THEN 'single_choice'
    WHEN question_type = 'MULTIPLE_CHOICE' THEN 'multiple_choice'
    WHEN question_type = 'TRUE_FALSE' THEN 'true_false'
    WHEN question_type = 'FILL_BLANK' THEN 'fill_blank'
    WHEN question_type = 'CODE_SNIPPET' THEN 'code_snippet'
    ELSE question_type
END
WHERE question_type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE', 'FILL_BLANK', 'CODE_SNIPPET');

-- 验证更新结果
SELECT question_type, COUNT(*) as count
FROM review_agent.quiz_question
GROUP BY question_type;
