package com.review.agent.entity.request;

import lombok.Data;

import java.util.List;

/**
 * 分析请求
 */
@Data
public class AnalysisRequest {
    private Long userId;
    private List<Long> fileIdList;
}