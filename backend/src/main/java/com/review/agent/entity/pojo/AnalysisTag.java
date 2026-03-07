package com.review.agent.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 分析结果-标签关联表（新版）
 * 支持多维度标签关联
 */
@Getter
@Setter
@Entity
@Table(name = "analysis_tag", schema = "review_agent")
public class AnalysisTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 分析结果ID
     */
    @Column(name = "analysis_result_id", nullable = false)
    private Long analysisResultId;

    /**
     * 标签ID
     */
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    /**
     * 标签（关联）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", insertable = false, updatable = false)
    private Tag tag;

    /**
     * 是否主标签
     */
    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    /**
     * AI匹配置信度（1-100）
     */
    @Column(name = "confidence")
    private Integer confidence = 100;

    @Column(name = "created_time", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @PrePersist
    public void prePersist() {
        this.createdTime = LocalDateTime.now();
        if (this.isPrimary == null) {
            this.isPrimary = false;
        }
        if (this.confidence == null) {
            this.confidence = 100;
        }
    }
}
