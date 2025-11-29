package com.review.agent.entity.request;

import lombok.Data;

import java.util.Date;

/**
 * 数据信息请求
 */
@Data
public class DataInfoRequest {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 处理状态（0=未分析, 1=已分析 2=正在分析 3=分析失败）
     */
    private Integer processedStatus;
    /**
     * 开始时间
     */
    private Date startTime;
    /**
     * 结束时间
     */
    private Date endTime;
}