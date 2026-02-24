package com.review.agent.entity.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 稍后复习请求对象
 */
@Data
public class SnoozeReviewRequest {

    @NotNull(message = "错题ID不能为空")
    private Long mistakeId;

    /**
     * 延迟小时数：1-24小时，默认24小时
     */
    @Min(value = 1, message = "延迟时间至少1小时")
    @Max(value = 24, message = "延迟时间最多24小时")
    private Integer hours = 24;
}
