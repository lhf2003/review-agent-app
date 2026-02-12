package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能推荐合集响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartRecommendationVO {

    /**
     * 推荐列表
     */
    private List<RecommendationItem> recommendations;

    /**
     * 总未归档分析结果数
     */
    private Integer totalUnarchived;

    /**
     * 单个推荐项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationItem {
        /**
         * 推荐ID（用于忽略操作）
         */
        private String recommendationId;

        /**
         * 建议的合集名称
         */
        private String suggestedName;

        /**
         * 建议的合集描述
         */
        private String suggestedDescription;

        /**
         * 主标签ID
         */
        private Long tagId;

        /**
         * 主标签名称
         */
        private String tagName;

        /**
         * 关联分析结果数量
         */
        private Integer analysisCount;

        /**
         * 关联的分析结果列表
         */
        private List<AnalysisResultPreview> analysisResults;

        /**
         * 推荐置信度（0-1）
         */
        private Double confidence;
    }

    /**
     * 分析结果预览
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisResultPreview {
        /**
         * 分析结果ID
         */
        private Long id;

        /**
         * 问题陈述
         */
        private String problemStatement;

        /**
         * 创建时间
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
        private LocalDateTime createdTime;
    }
}
