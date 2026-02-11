package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 复习推荐VO
 * 基于艾宾浩斯遗忘曲线的智能复习推荐
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRecommendationVO {

    /**
     * 错题记录ID
     */
    private Long mistakeId;

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
     * 知识点
     */
    private String knowledgePoint;

    /**
     * 错误次数
     */
    private Integer mistakeCount;

    /**
     * 最后复习时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime lastReviewTime;

    /**
     * 下次复习日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime nextReviewDate;

    /**
     * 推荐优先级（数值越高越优先）
     */
    private Integer priority;
}
