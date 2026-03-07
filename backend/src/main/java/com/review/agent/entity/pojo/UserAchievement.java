package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户成就表
 */
@Getter
@Setter
@Entity
@Table(name = "user_achievement")
public class UserAchievement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "成就代码不能为空")
    @Column(name = "achievement_code", nullable = false, length = 50)
    private String achievementCode;

    @Column(name = "unlocked", columnDefinition = "TINYINT")
    private Boolean unlocked;

    @Column(name = "progress")
    private Integer progress;

    @Column(name = "unlocked_time")
    private LocalDateTime unlockedTime;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

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
        if (this.unlocked == null) {
            this.unlocked = false;
        }
        if (this.progress == null) {
            this.progress = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
}
