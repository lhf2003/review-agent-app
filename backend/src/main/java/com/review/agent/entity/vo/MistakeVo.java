package com.review.agent.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题本视图对象
 * 用于返回错题列表和详情信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeVo {

    /**
     * 错题记录ID
     */
    private Long id;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 题目内容
     */
    private String questionText;

    /**
     * 题型：single_choice, multiple_choice, true_false, fill_blank, code_snippet
     */
    private String questionType;

    /**
     * 选项列表（JSON数组字符串）
     */
    private String optionsJson;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 答案解析
     */
    private String explanation;

    /**
     * 知识点
     */
    private String knowledgePoint;

    /**
     * 错误次数
     */
    private Integer mistakeCount;

    /**
     * 最后错误时间
     */
    private LocalDateTime lastMistakeTime;

    /**
     * 是否已掌握
     */
    private Boolean mastered;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
