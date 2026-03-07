package com.review.agent.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户思维范式掌握度表
 */
@Getter
@Setter
@Entity
@Table(name = "user_paradigm_mastery", schema = "review_agent")
public class UserParadigmMastery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 范式编码
     */
    @Column(name = "paradigm_code", nullable = false, length = 50)
    private String paradigmCode;

    /**
     * 使用次数
     */
    @Column(name = "usage_count")
    private Integer usageCount = 0;

    /**
     * 掌握度(0-100)
     */
    @Column(name = "mastery_score", precision = 5, scale = 2)
    private BigDecimal masteryScore = BigDecimal.ZERO;

    /**
     * 理解度(0-100)
     */
    @Column(name = "understanding_score")
    private Integer understandingScore = 0;

    /**
     * 应用能力(0-100)
     */
    @Column(name = "application_score")
    private Integer applicationScore = 0;

    /**
     * 最后使用时间
     */
    @Column(name = "last_used_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastUsedTime;

    @Column(name = "created_time", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @Column(name = "updated_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    @PrePersist
    public void prePersist() {
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
        if (this.usageCount == null) {
            this.usageCount = 0;
        }
        if (this.masteryScore == null) {
            this.masteryScore = BigDecimal.ZERO;
        }
        if (this.understandingScore == null) {
            this.understandingScore = 0;
        }
        if (this.applicationScore == null) {
            this.applicationScore = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
}
