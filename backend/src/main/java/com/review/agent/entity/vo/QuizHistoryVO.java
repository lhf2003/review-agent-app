package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 习题历史视图对象
 * 用于习题历史列表展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizHistoryVO {
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
     * 题目总数
     */
    private Integer questionCount;

    /**
     * 正确题目数
     */
    private Integer correctCount;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime createdTime;
}
