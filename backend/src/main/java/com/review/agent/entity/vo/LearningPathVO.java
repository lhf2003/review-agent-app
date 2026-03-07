package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习路径VO
 * 基于标签关系构建的个性化学习路径
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningPathVO {

    /**
     * 路径ID（目标标签ID）
     */
    private Long id;

    /**
     * 路径标题（目标标签名称）
     */
    private String title;

    /**
     * 路径描述
     */
    private String description;

    /**
     * 路径类型：sequential（顺序学习）、parallel（并行学习）、mastery（精通路线）
     */
    private String pathType;

    /**
     * 难度等级：beginner、intermediate、advanced、expert
     */
    private String difficulty;

    /**
     * 学习步骤列表
     */
    private List<LearningStepVO> steps;

    /**
     * 前置知识点（需要先掌握的）
     */
    private List<PathNodeVO> prerequisites;

    /**
     * 推荐并行学习的标签
     */
    private List<PathNodeVO> parallelTags;

    /**
     * 进阶方向
     */
    private List<PathNodeVO> nextSteps;

    /**
     * 路径完成度（0-100）
     */
    private Integer completionRate;

    /**
     * 预计学习时间（分钟）
     */
    private Integer estimatedMinutes;

    /**
     * 关联的题目数量
     */
    private Integer questionCount;

    /**
     * 推荐原因
     */
    private String reason;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /**
     * 学习路径中的单个步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LearningStepVO {
        /**
         * 步骤序号
         */
        private Integer order;

        /**
         * 标签ID
         */
        private Long tagId;

        /**
         * 标签名称
         */
        private String tagName;

        /**
         * 步骤类型：prerequisite（前置）、core（核心）、extension（扩展）
         */
        private String stepType;

        /**
         * 是否已完成
         */
        private Boolean completed;

        /**
         * 掌握度（0-100）
         */
        private Integer masteryLevel;

        /**
         * 相关题目数量
         */
        private Integer questionCount;

        /**
         * 描述
         */
        private String description;
    }

    /**
     * 路径节点
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathNodeVO {
        /**
         * 标签ID
         */
        private Long tagId;

        /**
         * 标签名称
         */
        private String tagName;

        /**
         * 掌握度（0-100）
         */
        private Integer masteryLevel;

        /**
         * 关系强度（0-100）
         */
        private Integer relationStrength;

        /**
         * 关系类型
         */
        private String relationType;
    }
}
