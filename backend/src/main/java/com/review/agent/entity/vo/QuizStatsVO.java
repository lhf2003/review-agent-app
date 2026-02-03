package com.review.agent.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 习题统计数据 VO
 * 用于习题历史页面的统计图表展示
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizStatsVO {
    /**
     * 测验分数趋势
     */
    private List<QuizScoreTrendVo> quizScoreTrend;

    /**
     * 知识点掌握度
     */
    private List<KnowledgeMasteryVo> knowledgeMastery;

    /**
     * 测验分数趋势 VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizScoreTrendVo {
        /**
         * 测验日期（yyyy-MM-dd）
         */
        private String date;

        /**
         * 分数
         */
        private Integer score;
    }

    /**
     * 知识点掌握度 VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeMasteryVo {
        /**
         * 标签名
         */
        private String tagName;

        /**
         * 正确率（百分比）
         */
        private Double accuracyRate;
    }
}
