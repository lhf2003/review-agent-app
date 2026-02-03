package com.review.agent.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户统计数据 VO
 */
@Data
public class UserStatsVo {
    /**
     * 已同步文件数
     */
    private Long syncFileCount;

    /**
     * 已分析结果数
     */
    private Long analyzedCount;

    /**
     * 合集数量
     */
    private Long collectionCount;

    /**
     * 标签数量 (主标签 + 子标签)
     */
    private Long tagCount;

    /**
     * 测验完成数
     */
    private Long quizCompletedCount;

    /**
     * 学习天数
     */
    private Long learningDays;

    /**
     * 最近活动列表
     */
    private List<RecentActivityVo> recentActivities;

    /**
     * 成就列表
     */
    private List<AchievementVo> achievements;

    /**
     * 学习进度
     */
    private LearningProgressVo learningProgress;

    /**
     * 最近活动 VO
     */
    @Data
    public static class RecentActivityVo {
        /**
         * 活动类型: sync, collection, quiz, report
         */
        private String type;

        /**
         * 活动时间
         */
        private String time;

        /**
         * 活动详情
         */
        private String detail;
    }

    /**
     * 成就 VO
     */
    @Data
    public static class AchievementVo {
        /**
         * 成就代码
         */
        private String code;

        /**
         * 成就名称
         */
        private String name;

        /**
         * 成就描述
         */
        private String description;

        /**
         * 成就图标
         */
        private String icon;

        /**
         * 是否已解锁
         */
        private Boolean unlocked;

        /**
         * 当前进度
         */
        private Integer progress;

        /**
         * 目标值（用于进度条）
         */
        private Integer target;

        /**
         * 解锁时间
         */
        private String unlockedTime;
    }

    /**
     * 学习进度 VO
     */
    @Data
    public static class LearningProgressVo {
        /**
         * 总体进度（百分比）
         */
        private Integer overallProgress;

        /**
         * 同步文件进度（百分比）
         */
        private Integer syncProgress;

        /**
         * 已分析结果进度（百分比）
         */
        private Integer analysisProgress;

        /**
         * 测验完成进度（百分比）
         */
        private Integer quizProgress;

        /**
         * 成就解锁进度（百分比）
         */
        private Integer achievementProgress;
    }
}
