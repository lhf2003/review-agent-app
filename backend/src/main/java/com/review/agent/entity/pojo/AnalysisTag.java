package com.review.agent.entity.pojo;

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
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "analysis_id", nullable = false)
    private Long analysisId;

    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "sub_tag_id")
    private String subTagId;

    @Column(name = "recommends")
    private String recommends;

    @Column(name = "confidence_score")
    private Double confidenceScore;

}
