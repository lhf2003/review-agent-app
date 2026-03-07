package com.review.agent.entity.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 知识图谱节点 VO
 */
@Data
public class SimpleNodeVO {

    /**
     * 节点唯一标识（知识点名称）
     */
    private String id;

    /**
     * 节点显示名称
     */
    private String name;

    /**
     * 掌握度分数 (0-100)
     */
    private BigDecimal masteryScore;

    /**
     * 所属分组（主标签）
     */
    private String group;

    /**
     * 节点大小（根据答题次数计算）
     */
    private Integer symbolSize;

    /**
     * 节点颜色（根据掌握度计算）
     * #ff4d4f=薄弱, #faad14=一般, #52c41a=良好
     */
    private String color;

    /**
     * 答题次数
     */
    private Integer totalAnswered;

    /**
     * 错误次数
     */
    private Integer mistakeCount;
}
