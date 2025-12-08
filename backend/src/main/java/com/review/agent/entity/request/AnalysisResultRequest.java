package com.review.agent.entity.request;

import lombok.Data;

@Data
public class AnalysisResultRequest {
    private Long userId;
    private Long fileId;
    private String problemStatement;
    private Long tagId;
}