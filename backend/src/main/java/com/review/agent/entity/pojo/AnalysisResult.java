package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "analysis_result", schema = "review_agent")
public class AnalysisResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "vector_id")
    private String vectorId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "problem_statement")
    private String problemStatement;

    @Column(name = "solution", columnDefinition = "LONGTEXT")
    private String solution;

    @Column(name = "session_start", columnDefinition = "TINYINT")
    private Integer sessionStart;

    @Column(name = "session_end", columnDefinition = "TINYINT")
    private Integer sessionEnd;

    @Column(name = "session_content", columnDefinition = "LONGTEXT")
    private String sessionContent;

    @NotNull
    @Column(name = "status", nullable = false, columnDefinition = "TINYINT")
    private Integer status;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    /**
     * 删除标记（软删除）
     * false-未删除，true-已删除
     */
    @ColumnDefault("false")
    @Column(name = "deleted")
    private Boolean deleted = false;

    /**
     * 删除时间（软删除）
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}