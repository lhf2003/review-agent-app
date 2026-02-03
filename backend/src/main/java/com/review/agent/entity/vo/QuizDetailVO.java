package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 习题详情视图对象
 * 用于习题详情页面展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDetailVO {
    /**
     * 习题记录ID
     */
    private Long quizId;

    /**
     * 合集ID
     */
    private Long collectionId;

    /**
     * 合集名称
     */
    private String collectionName;

    /**
     * 总分（百分制）
     */
    private Integer totalScore;

    /**
     * 状态（0=进行中，1=已完成）
     */
    private Integer status;

    /**
     * 题库是否已过期
     */
    private Boolean isOutdated;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime createdTime;

    /**
     * 题目详情列表
     */
    private List<QuestionDetailVO> questions;
}
