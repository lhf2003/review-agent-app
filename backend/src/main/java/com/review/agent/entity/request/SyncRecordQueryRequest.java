package com.review.agent.entity.request;

import lombok.Data;

import java.util.Date;

/**
 * 同步记录查询请求
 */
@Data
public class SyncRecordQueryRequest {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 同步状态：0=成功, 1=同步中, 2=失败
     */
    private Integer status;

    /**
     * 开始时间（格式：yyyy-MM-dd）
     */
    private String startDate;

    /**
     * 结束时间（格式：yyyy-MM-dd）
     */
    private String endDate;

    /**
     * 页码（从 0 开始）
     */
    private Integer page;

    /**
     * 每页大小
     */
    private Integer size;
}
