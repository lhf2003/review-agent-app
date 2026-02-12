package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 学习数据可视化仪表盘 VO
 * 包含测验分数趋势、知识点掌握度、学习热力图、时间分布和周统计数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningDashboardVO {

    /**
     * 测验分数趋势
     */
    private List<ScoreTrendItem> scoreTrend;

    /**
     * 知识点雷达图数据
     */
    private List<KnowledgeRadarItem> knowledgeRadar;

    /**
     * 学习热力图数据
     * key: 日期 (yyyy-MM-dd)
     * value: 当日测验完成数量
     */
    private Map<String, Integer> heatmap;

    /**
     * 学习时长分布
     * key: 时段名称（凌晨、上午、下午、晚上）
     * value: 测验完成数量
     */
    private Map<String, Integer> timeDistribution;

    /**
     * 周统计数据（本周 vs 上周对比）
     */
    private WeeklyStats weeklyStats;

    /**
     * 测验分数趋势项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreTrendItem {
        /**
         * 测验日期
         */
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDateTime date;

        /**
         * 测验分数（0-100）
         */
        private Integer score;

        /**
         * 测验记录ID（用于跳转详情）
         */
        private Long quizId;

        /**
         * 合集名称
         */
        private String collectionName;
    }

    /**
     * 知识点雷达图项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeRadarItem {
        /**
         * 知识点名称
         */
        private String name;

        /**
         * 正确率（0-100）
         */
        private Double value;

        /**
         * 总答题数
         */
        private Integer totalCount;
    }

    /**
     * 周统计数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyStats {
        /**
         * 本周测验完成数
         */
        private Integer thisWeekQuizCount;

        /**
         * 上周测验完成数
         */
        private Integer lastWeekQuizCount;

        /**
         * 本周正确率
         */
        private Double thisWeekAccuracy;

        /**
         * 上周正确率
         */
        private Double lastWeekAccuracy;

        /**
         * 本周测验数变化（正数为增长，负数为下降）
         */
        private Integer quizCountChange;

        /**
         * 正确率变化（正数为提升，负数为下降）
         */
        private Double accuracyChange;
    }
}
