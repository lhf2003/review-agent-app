package com.review.agent.entity.vo;

import lombok.Data;

/**
 * 模型提供商 VO
 */
@Data
public class LlmProviderVo{
    private Integer id;

    private String name;

    private String requestUrl;

    private String apiKey;
}