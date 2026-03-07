package com.review.agent.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 标签维度定义
 * 支持多维度标签体系：技术领域、思维范式、难度等级、应用场景
 */
@Getter
@Setter
@Entity
@Table(name = "tag_dimension", schema = "review_agent")
public class TagDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 维度名称
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * 维度编码（唯一）
     * TECH_DOMAIN: 技术领域
     * THINKING_PARADIGM: 思维范式
     * DIFFICULTY: 难度等级
     * SCENARIO: 应用场景
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 维度说明
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * 图标
     */
    @Column(name = "icon", length = 100)
    private String icon;

    /**
     * 排序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

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
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
}
