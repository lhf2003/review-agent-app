package com.review.agent.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 标签关系表（新）
 * 支持标签之间的多种关系类型：依赖、相似、互补、冲突、演进
 */
@Getter
@Setter
@Entity
@Table(name = "tag_relation", schema = "review_agent")
public class TagRelation {

    /**
     * 关系类型枚举
     */
    public enum RelationType {
        DEPENDS_ON("前置依赖", "需要先理解源标签才能学习目标标签"),
        SIMILAR_TO("概念相似", "两者思路类似，可以类比学习"),
        COMPLEMENTS("互补", "两者结合使用效果更好"),
        CONFLICTS_WITH("冲突", "两者不能同时使用或互斥"),
        EVOLVES_TO("演进", "源标签是目标标签的进化版");

        private final String label;
        private final String description;

        RelationType(String label, String description) {
            this.label = label;
            this.description = description;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 源标签ID
     */
    @Column(name = "source_tag_id", nullable = false)
    private Long sourceTagId;

    /**
     * 源标签（关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_tag_id", insertable = false, updatable = false)
    private Tag sourceTag;

    /**
     * 目标标签ID
     */
    @Column(name = "target_tag_id", nullable = false)
    private Long targetTagId;

    /**
     * 目标标签（关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_tag_id", insertable = false, updatable = false)
    private Tag targetTag;

    /**
     * 关系类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 50)
    private RelationType relationType;

    /**
     * 关系强度（1-100）
     */
    @Column(name = "strength")
    private Integer strength = 50;

    /**
     * 关系依据说明
     */
    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    /**
     * 是否AI自动发现
     */
    @Column(name = "is_auto_detected")
    private Boolean isAutoDetected = false;

    /**
     * 用户ID（系统预设关系可为null）
     */
    @Column(name = "user_id")
    private Long userId;

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
        if (this.strength == null) {
            this.strength = 50;
        }
        if (this.isAutoDetected == null) {
            this.isAutoDetected = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
}
