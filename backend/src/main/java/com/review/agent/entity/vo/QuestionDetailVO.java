package com.review.agent.entity.vo;

import com.review.agent.common.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 题目详情视图对象
 * 用于习题详情页面展示单个题目
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDetailVO {
    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 题目文本
     */
    private String questionText;

    /**
     * 题目类型
     */
    private QuestionType questionType;

    /**
     * 选项JSON（单选、多选题）
     */
    private String optionsJson;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 用户答案
     */
    private String userAnswer;

    /**
     * 是否正确
     */
    private Boolean isCorrect;

    /**
     * 解析
     */
    private String explanation;

    /**
     * 知识点
     */
    private String knowledgePoint;

    /**
     * 难度等级（1-5）
     */
    private Integer difficultyLevel;

    /**
     * 时间限制（秒）
     */
    private Integer timeLimit;

    /**
     * 填空数量（填空题）
     */
    private Integer blankCount;

    /**
     * 关联的分析结果ID
     */
    private Long relatedAnalysisId;
}
