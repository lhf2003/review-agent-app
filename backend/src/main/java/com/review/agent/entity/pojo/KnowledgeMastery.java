package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识点掌握度实体
 * 追踪用户对各知识点的掌握程度
 */
@Getter
@Setter
@Entity
@Table(name = "knowledge_mastery", schema = "review_agent",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_knowledge_point",
        columnNames = {"user_id", "knowledge_point"}
    )
)
public class KnowledgeMastery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 标签ID（与tag表关联）
     */
    @Column(name = "tag_id")
    private Long tagId;

    @NotNull(message = "知识点不能为空")
    @Column(name = "knowledge_point", nullable = false, length = 100)
    private String knowledgePoint;

    @Column(name = "total_answered")
    private Integer totalAnswered = 0;

    @Column(name = "correct_count")
    private Integer correctCount = 0;

    @Column(name = "mastery_score", precision = 5, scale = 2)
    private BigDecimal masteryScore = BigDecimal.ZERO;

    @Column(name = "average_time")
    private Integer averageTime;

    @Column(name = "last_practice_time")
    private LocalDateTime lastPracticeTime;

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
        if (this.totalAnswered == null) {
            this.totalAnswered = 0;
        }
        if (this.correctCount == null) {
            this.correctCount = 0;
        }
        if (this.masteryScore == null) {
            this.masteryScore = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 获取掌握度等级（0-100的整数）
     * 将 masteryScore (BigDecimal) 转换为 int 等级
     */
    public int getMasteryLevel() {
        if (masteryScore == null) {
            return 0;
        }
        return masteryScore.intValue();
    }
}
