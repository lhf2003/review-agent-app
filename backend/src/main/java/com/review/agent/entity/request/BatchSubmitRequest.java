package com.review.agent.entity.request;

import lombok.Data;

import java.util.List;

/**
 * 批量提交答案请求
 */
@Data
public class BatchSubmitRequest {
    /**
     * 测验记录ID
     */
    private Long quizId;

    /**
     * 答案列表
     */
    private List<QuestionAnswer> answers;

    /**
     * 单个题目的答案
     */
    @Data
    public static class QuestionAnswer {
        /**
         * 题目ID
         */
        private Long questionId;

        /**
         * 用户答案
         */
        private String userAnswer;

        /**
         * 答题用时（秒）
         */
        private Integer timeSpent;
    }
}
