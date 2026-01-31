package com.review.agent.repository;

import com.review.agent.entity.pojo.CollectionRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CollectionRelationRepository extends JpaRepository<CollectionRelation, Long> {
    
    List<CollectionRelation> findByCollectionId(Long collectionId);

    @Query("select count(c) from CollectionRelation c where c.collectionId = :collectionId")
    long countByCollectionId(Long collectionId);

    @Modifying
    @Transactional
    @Query("delete from CollectionRelation c where c.collectionId = :collectionId")
    void deleteByCollectionId(Long collectionId);

    @Modifying
    @Transactional
    @Query("delete from CollectionRelation c where c.collectionId = :collectionId and c.analysisResultId in :analysisIds")
    void deleteByCollectionIdAndAnalysisIdIn(Long collectionId, List<Long> analysisIds);

    @Query("select c.collectionId from CollectionRelation c where c.analysisResultId = :analysisResultId")
    List<Long> findCollectionIdsByAnalysisResultId(Long analysisResultId);

    boolean existsByCollectionIdAndAnalysisResultId(Long collectionId, Long analysisResultId);

    /**
     * Batch query counts for multiple collections at once to avoid N+1 queries
     * Returns a map of collectionId -> count
     */
    @Query("select c.collectionId, count(c) from CollectionRelation c where c.collectionId in :collectionIds group by c.collectionId")
    List<Object[]> countByCollectionIds(List<Long> collectionIds);
}
