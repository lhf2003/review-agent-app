package com.review.agent.repository;

import com.review.agent.entity.pojo.AnalysisCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnalysisCollectionRepository extends JpaRepository<AnalysisCollection, Long> {
    List<AnalysisCollection> findByUserIdOrderByCreatedTimeDesc(Long userId);

    @Query("select c from AnalysisCollection c where c.id = :collectionId and c.userId = :userId")
    AnalysisCollection findByIdAndUserId(Long userId, Long collectionId);
}
