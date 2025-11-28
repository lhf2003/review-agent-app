package com.review.agent.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "analysis_tag", schema = "review_agent")
public class AnalysisTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "tag_id", nullable = false)
    private Integer tagId;

    @Column(name = "confidence_score")
    private Double confidenceScore;

}