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
}
