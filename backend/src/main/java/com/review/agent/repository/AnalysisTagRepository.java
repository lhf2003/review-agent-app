package com.review.agent.repository;

import com.review.agent.entity.AnalysisTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisTagRepository extends JpaRepository<AnalysisTag, Long> {
    long countByTagId(Long tagId);
}
