package com.review.agent.entity.vo;

import lombok.Data;

/**
 * 提交答案结果 VO
 */
@Data
public class SubmitAnswerResultVO {

    /**
     * 是否正确
     */
    private Boolean correct;

    /**
     * 用户答案
     */
    private String userAnswer;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 解析
     */
    private String explanation;

    /**
     * 知识点
     */
    private String knowledgePoint;

    public SubmitAnswerResultVO() {
    }

    public SubmitAnswerResultVO(Boolean correct, String userAnswer, String correctAnswer, String explanation, String knowledgePoint) {
        this.correct = correct;
        this.userAnswer = userAnswer;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.knowledgePoint = knowledgePoint;
    }
}
