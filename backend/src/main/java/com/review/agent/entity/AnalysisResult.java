package com.review.agent.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.Date;

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

    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "keyword")
    private String keyword;

    @Column(name = "problem_statement")
    private String problemStatement;

    @Lob
    @Column(name = "solution")
    private String solution;

    @NotNull
    @Column(name = "status", nullable = false)
    private Integer status;

    @Temporal(TemporalType.TIMESTAMP)
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_time")
    private Date createdTime;

}