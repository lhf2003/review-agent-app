package com.review.agent.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 标签表（支持无限层级和多维度）
 * 替代原有的 MainTag 和 SubTag
 */
@Getter
@Setter
@Entity
@Table(name = "tag", schema = "review_agent")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 标签名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 所属维度ID
     */
    @Column(name = "dimension_id", nullable = false)
    private Long dimensionId;

    /**
     * 所属维度（关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dimension_id", insertable = false, updatable = false)
    private TagDimension dimension;

    /**
     * 父标签ID，null表示根标签
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 父标签（关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    private Tag parent;

    /**
     * 层级深度（建议1-4层）
     */
    @Column(name = "level", nullable = false)
    private Integer level = 1;

    /**
     * 完整路径，如：/编程/Java/Spring
     */
    @Column(name = "path", length = 500)
    private String path;

    // ========== 思维范式专用字段 ==========

    /**
     * 范式编码（仅THINKING_PARADIGM维度使用）
     * DECOMPOSITION: 问题分解
     * ANALOGY: 类比推理
     * FIRST_PRINCIPLES: 第一性原理
     * PATTERN_RECOGNITION: 模式识别
     * SYSTEMATIC_DEBUGGING: 系统调试
     * TRADE_OFF_ANALYSIS: 权衡分析
     * ABSTRACTION_MODELING: 抽象建模
     */
    @Column(name = "paradigm_code", length = 50)
    private String paradigmCode;

    /**
     * 范式说明
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 适用场景描述
     */
    @Column(name = "when_to_use", columnDefinition = "TEXT")
    private String whenToUse;

    /**
     * 典型案例
     */
    @Column(name = "example", columnDefinition = "TEXT")
    private String example;

    // ========== 通用字段 ==========

    /**
     * 用户ID（系统内置标签可为null）
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
        if (this.level == null) {
            this.level = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 检查是否为思维范式标签
     */
    public boolean isThinkingParadigm() {
        return this.paradigmCode != null && !this.paradigmCode.isEmpty();
    }

    /**
     * 检查是否为系统内置标签
     */
    public boolean isSystemTag() {
        return this.userId == null;
    }
}
