package com.review.agent.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 分析结果推荐标签表
 * 存储AI分析时LLM建议的新标签，支持用户采纳或忽略
 */
@Getter
@Setter
@Entity
@Table(name = "analysis_recommend_tag", schema = "review_agent")
public class AnalysisRecommendTag {

    /**
     * 用户操作状态枚举
     */
    public enum UserAction {
        PENDING,   // 待处理
        ADOPTED,   // 已采纳
        IGNORED    // 已忽略
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 关联的分析结果ID
     */
    @Column(name = "analysis_result_id", nullable = false)
    private Long analysisResultId;

    /**
     * 推荐标签名称
     */
    @Column(name = "tag_name", nullable = false, length = 100)
    private String tagName;

    /**
     * 用户操作状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_action", nullable = false, length = 20)
    private UserAction userAction = UserAction.PENDING;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
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
        if (this.userAction == null) {
            this.userAction = UserAction.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }

    /**
     * 检查是否为待处理状态
     */
    public boolean isPending() {
        return this.userAction == UserAction.PENDING;
    }

    /**
     * 检查是否已采纳
     */
    public boolean isAdopted() {
        return this.userAction == UserAction.ADOPTED;
    }

    /**
     * 检查是否已忽略
     */
    public boolean isIgnored() {
        return this.userAction == UserAction.IGNORED;
    }
}
