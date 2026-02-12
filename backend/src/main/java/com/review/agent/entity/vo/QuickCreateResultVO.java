package com.review.agent.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 快速创建合集响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickCreateResultVO {

    /**
     * 创建的合集ID
     */
    private Long collectionId;

    /**
     * 包含的分析结果数量
     */
    private Integer createdCount;

    /**
     * 新解锁的成就（暂未实现）
     */
    private List<Object> newlyUnlockedAchievements;
}
