package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 合集推荐VO
 * 用于智能学习路径推荐功能
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRecommendationVO {

    /**
     * 合集ID
     */
    private Long id;

    /**
     * 合集名称
     */
    private String name;

    /**
     * 合集描述
     */
    private String description;

    /**
     * 关联的分析结果数量
     */
    private Long count;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    /**
     * 合集标签列表
     */
    private List<String> tags;

    /**
     * 匹配度（0-100）
     */
    private Integer matchRate;

    /**
     * 推荐原因
     */
    private String reason;

    /**
     * 推荐时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime recommendedAt;
}
