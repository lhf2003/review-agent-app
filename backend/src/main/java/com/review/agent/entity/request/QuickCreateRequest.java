package com.review.agent.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 快速创建合集请求
 */
@Data
public class QuickCreateRequest {

    /**
     * 合集名称
     */
    @NotBlank(message = "合集名称不能为空")
    private String name;

    /**
     * 合集描述
     */
    private String description;

    /**
     * 分析结果ID列表
     */
    @NotEmpty(message = "分析结果列表不能为空")
    private List<Long> analysisIds;
}
