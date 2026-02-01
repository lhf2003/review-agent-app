package com.review.agent.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建合集结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCreateResultVo {
    /**
     * 合集ID
     */
    private Long collectionId;

    /**
     * 新解锁的成就列表（成就完整信息）
     */
    private List<UserStatsVo.AchievementVo> newlyUnlockedAchievements;
}
