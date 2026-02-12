package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待复习项 VO
 */
@Data
@Builder
public class PendingReviewVO {

    /**
     * 类型：MISTAKE=错题，QUIZ=习题
     */
    private String type;

    /**
     * 关联ID（错题ID或习题记录ID）
     */
    private Long referenceId;

    /**
     * 标题/描述
     */
    private String title;

    /**
     * 知识点
     */
    private String knowledgePoint;

    /**
     * 复习优先级（1-5，5最高）
     */
    private Integer priority;

    /**
     * 上次复习/创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime lastReviewTime;

    /**
     * 下次应复习时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime nextReviewTime;

    /**
     * 遗忘曲线阶段（第几天复习）
     */
    private Integer reviewStage;

    /**
     * 错误次数（仅错题类型）
     */
    private Integer mistakeCount;

    /**
     * 习题分数（仅习题类型）
     */
    private Integer quizScore;
}
