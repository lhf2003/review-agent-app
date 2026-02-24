package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 错题答题历史实体
 * 记录用户每次答题的详细信息
 */
@Getter
@Setter
@Entity
@Table(name = "quiz_mistake_history", schema = "review_agent")
public class MistakeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "mistake_id", nullable = false)
    private Long mistakeId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "quiz_id")
    private Long quizId;

    @Column(name = "wrong_answer", length = 500)
    private String wrongAnswer;

    @Column(name = "correct_answer", length = 500)
    private String correctAnswer;

    @Column(name = "time_spent")
    private Integer timeSpent;

    @Column(name = "is_correct")
    private Boolean isCorrect = false;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @PrePersist
    public void prePersist() {
        if (this.createdTime == null) {
            this.createdTime = LocalDateTime.now();
        }
        if (this.isCorrect == null) {
            this.isCorrect = false;
        }
    }
}
