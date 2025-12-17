package com.review.agent.entity.request;

import lombok.Data;

@Data
public class TagRecommendRequest {
    /**
     * 标签名称
     */
    private String name;
    /**
     * 分析结果id
     */
    private Long analysisId;
    /**
     * 推荐标签类型（1：主标签 2：子标签）
     */
    private Integer tagType;
}