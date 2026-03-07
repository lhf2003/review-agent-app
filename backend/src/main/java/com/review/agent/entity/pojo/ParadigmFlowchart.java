package com.review.agent.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 思维范式流程图存储表
 */
@Getter
@Setter
@Entity
@Table(name = "paradigm_flowchart", schema = "review_agent")
public class ParadigmFlowchart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 范式编码
     */
    @Column(name = "paradigm_code", nullable = false, length = 50)
    private String paradigmCode;

    /**
     * 关联的分析结果ID
     */
    @Column(name = "analysis_result_id", nullable = false)
    private Long analysisResultId;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 流程图标题
     */
    @Column(name = "title", length = 200)
    private String title;

    /**
     * 流程图描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 范式名称
     */
    @Column(name = "paradigm_name", length = 100)
    private String paradigmName;

    /**
     * 节点列表（JSON格式）
     */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "nodes", columnDefinition = "json")
    private String nodes;

    /**
     * 边列表（JSON格式）
     */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "edges", columnDefinition = "json")
    private String edges;

    /**
     * 布局配置（JSON格式）
     */
    @Lob
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "layout", columnDefinition = "json")
    private String layout;

    /**
     * 范式类型：FLOW_CHART/DECISION_TREE/DECOMPOSITION_TREE/DERIVATION_CHAIN
     */
    @Column(name = "paradigm_type", nullable = false, length = 50)
    private String paradigmType;

    /**
     * 节点数量
     */
    @Column(name = "node_count")
    private Integer nodeCount = 0;

    /**
     * 复杂度评分(1-100)
     */
    @Column(name = "complexity_score")
    private Integer complexityScore;

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
        if (this.nodeCount == null) {
            this.nodeCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedTime = LocalDateTime.now();
    }
}
