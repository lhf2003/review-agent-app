package com.review.agent.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识点掌握度VO
 * 用于统计用户对各个知识点的掌握程度
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeMasteryVO {

    /**
     * 知识点名称
     */
    private String knowledgePoint;

    /**
     * 掌握度（0-100）
     */
    private Double masteryRate;

    /**
     * 答对次数
     */
    private Integer correctCount;

    /**
     * 总答题次数
     */
    private Integer totalCount;
}
