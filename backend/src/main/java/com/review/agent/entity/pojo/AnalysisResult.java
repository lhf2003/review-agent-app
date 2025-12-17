package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

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

    @Column(name = "problem_statement")
    private String problemStatement;

    @Lob
    @Column(name = "solution")
    private String solution;

    @Column(name = "session_start")
    private Integer sessionStart;

    @Column(name = "session_end")
    private Integer sessionEnd;

    @Column(name = "session_content")
    private String sessionContent;

    @NotNull
    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "created_time")
    private Date createdTime;

}