package com.review.agent.entity.request;

import lombok.Data;

@Data
public class AnalysisResultRequest {
    private Long userId;
    private Long analysisId;
    private String problemStatement;
    private Long tagId;
    private Integer status;

}