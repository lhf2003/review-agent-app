package com.review.agent.entity.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 多维度标签分类结果
 * 支持技术领域、思维范式、难度等级、应用场景四个维度的标签识别
 */
@Data
public class MultiDimensionTagResult {

    /**
     * 技术领域标签
     */
    private TechDomainResult techDomain;

    /**
     * 思维范式标签列表（可多选）
     */
    private List<ThinkingParadigmResult> thinkingParadigms = new ArrayList<>();

    /**
     * 难度等级标签
     */
    private DifficultyResult difficulty;

    /**
     * 应用场景标签
     */
    private ScenarioResult scenario;

    /**
     * 思维质量评估
     */
    private String thinkingQuality;

    /**
     * 探索路径描述
     */
    private String explorationPath;

    @Data
    public static class TechDomainResult {
        /**
         * 主分类ID
         */
        private Long mainTagId;

        /**
         * 主分类名称
         */
        private String mainTagName;

        /**
         * 子标签ID列表
         */
        private List<Long> subTagIds = new ArrayList<>();

        /**
         * 子标签名称列表
         */
        private List<String> subTagNames = new ArrayList<>();

        /**
         * 推荐标签
         */
        private List<String> recommends = new ArrayList<>();
    }

    @Data
    public static class ThinkingParadigmResult {
        /**
         * 范式编码
         */
        private String paradigmCode;

        /**
         * 范式名称
         */
        private String paradigmName;

        /**
         * 置信度（1-100）
         */
        private Integer confidence;

        /**
         * 具体应用描述
         */
        private String application;

        /**
         * 关键洞察
         */
        private String keyInsight;

        /**
         * 对应标签ID
         */
        private Long tagId;
    }

    @Data
    public static class DifficultyResult {
        /**
         * 难度等级ID
         */
        private Long difficultyId;

        /**
         * 难度等级名称
         */
        private String difficultyName;

        /**
         * 置信度
         */
        private Integer confidence;
    }

    @Data
    public static class ScenarioResult {
        /**
         * 应用场景ID
         */
        private Long scenarioId;

        /**
         * 应用场景名称
         */
        private String scenarioName;

        /**
         * 置信度
         */
        private Integer confidence;
    }
}
