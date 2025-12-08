package com.review.agent.entity.request;

import lombok.Data;

@Data
public class ReportRequest {
    private Long userId;
    /**
     * 0：一周
     * 1：一个月
     */
    private Integer flag;
}