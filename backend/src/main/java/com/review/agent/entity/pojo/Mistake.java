package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 错题本实体
 * 记录用户答错的题目及其掌握状态
 */
@Getter
@Setter
@Entity
@Table(name = "quiz_mistake", schema = "review_agent")
public class Mistake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "题目ID不能为空")
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "quiz_id")
    private Long quizId;

    @Column(name = "mistake_count")
    private Integer mistakeCount = 1;

    @Column(name = "last_mistake_time")
    private LocalDateTime lastMistakeTime;

    @Column(name = "mastered")
    private Boolean mastered = false;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @Column(name = "snoozed_until")
    private LocalDateTime snoozedUntil;

    @Column(name = "snooze_count")
    private Integer snoozeCount = 0;

    @PrePersist
    public void prePersist() {
        if (this.createdTime == null) {
            this.createdTime = LocalDateTime.now();
        }
        if (this.updatedTime == null) {
            this.updatedTime = LocalDateTime.now();
        }
        if (this.mistakeCount == null) {
            this.mistakeCount = 1;
        }
        if (this.mastered == null) {
            this.mastered = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
}
