package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知设置 VO
 */
@Data
@Builder
public class NotificationSettingsVO {

    /**
     * 是否启用浏览器通知
     */
    private Boolean browserNotificationEnabled;

    /**
     * 提醒频率（天数）
     */
    private Integer reminderFrequencyDays;

    /**
     * 提醒时间（小时，0-23）
     */
    private Integer reminderHour;

    /**
     * 是否启用错题复习提醒
     */
    private Boolean mistakeReviewEnabled;

    /**
     * 是否启用习题完成提醒
     */
    private Boolean quizCompletionEnabled;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime updatedTime;
}
