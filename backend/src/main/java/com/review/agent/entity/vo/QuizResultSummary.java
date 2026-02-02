package com.review.agent.entity.vo;

import lombok.Data;

/**
 * 答题结果统计
 */
@Data
public class QuizResultSummary {
    /**
     * 总题数
     */
    private Integer totalCount;

    /**
     * 答对题数
     */
    private Integer correctCount;

    /**
     * 答错题数
     */
    private Integer incorrectCount;

    /**
     * 未作答题数
     */
    private Integer unansweredCount;
}
