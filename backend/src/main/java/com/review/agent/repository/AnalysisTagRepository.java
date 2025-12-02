package com.review.agent.repository;

import com.review.agent.entity.AnalysisTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnalysisTagRepository extends JpaRepository<AnalysisTag, Long> {
    long countByTagId(Long tagId);

    @Query("select at from AnalysisTag at where at.analysisId in :analysisIdList")
    List<AnalysisTag> findByAnalysisIdIn(List<Long> analysisIdList);
}
