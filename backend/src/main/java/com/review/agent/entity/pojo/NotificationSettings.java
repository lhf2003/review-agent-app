package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 通知设置实体
 * 存储用户的复习提醒偏好设置
 */
@Getter
@Setter
@Entity
@Table(name = "notification_settings", schema = "review_agent")
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /**
     * 是否启用浏览器通知
     */
    @Column(name = "browser_notification_enabled")
    private Boolean browserNotificationEnabled = false;

    /**
     * 提醒频率（天数）：1=每天，3=每3天，7=每周
     */
    @Column(name = "reminder_frequency_days")
    private Integer reminderFrequencyDays = 1;

    /**
     * 提醒时间（小时，0-23）
     */
    @Column(name = "reminder_hour")
    private Integer reminderHour = 9;

    /**
     * 是否启用错题复习提醒
     */
    @Column(name = "mistake_review_enabled")
    private Boolean mistakeReviewEnabled = true;

    /**
     * 是否启用习题完成提醒
     */
    @Column(name = "quiz_completion_enabled")
    private Boolean quizCompletionEnabled = true;

    /**
     * 创建时间
     */
    @Column(name = "created_time")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @PrePersist
    public void prePersist() {
        if (this.createdTime == null) {
            this.createdTime = LocalDateTime.now();
        }
        if (this.updatedTime == null) {
            this.updatedTime = LocalDateTime.now();
        }
        if (this.browserNotificationEnabled == null) {
            this.browserNotificationEnabled = false;
        }
        if (this.reminderFrequencyDays == null) {
            this.reminderFrequencyDays = 1;
        }
        if (this.reminderHour == null) {
            this.reminderHour = 9;
        }
        if (this.mistakeReviewEnabled == null) {
            this.mistakeReviewEnabled = true;
        }
        if (this.quizCompletionEnabled == null) {
            this.quizCompletionEnabled = true;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
}
