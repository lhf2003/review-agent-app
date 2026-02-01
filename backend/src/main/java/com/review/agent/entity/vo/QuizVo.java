package com.review.agent.entity.vo;

import lombok.Data;
import java.util.List;

@Data
public class QuizVo {
    private Long id;
    private Long collectionId;
    private List<QuestionVo> questions;

    @Data
    public static class QuestionVo {
        private Long id;
        private String question;
        private String type; // 题目类型：single_choice, multiple_choice, true_false, fill_blank, code_snippet
        private List<String> options;
        private String answer;
        private String explanation;
        private String userAnswer;
        private String knowledgePoint; // 知识点
    }
}
