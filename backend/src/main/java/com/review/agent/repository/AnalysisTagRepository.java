package com.review.agent.repository;

import com.review.agent.entity.pojo.AnalysisTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 分析结果-标签关联Repository
 */
@Repository
public interface AnalysisTagRepository extends JpaRepository<AnalysisTag, Long> {

    /**
     * 根据分析结果ID查询关联
     */
    List<AnalysisTag> findByAnalysisResultId(Long analysisResultId);

    /**
     * 根据多个分析结果ID查询关联
     */
    List<AnalysisTag> findByAnalysisResultIdIn(List<Long> analysisResultIds);

    /**
     * 根据标签ID查询关联
     */
    List<AnalysisTag> findByTagId(Long tagId);

    /**
     * 根据分析结果和标签查询
     */
    AnalysisTag findByAnalysisResultIdAndTagId(Long analysisResultId, Long tagId);

    /**
     * 查询主标签
     */
    @Query("SELECT at FROM AnalysisTag at WHERE at.analysisResultId = :analysisResultId AND at.isPrimary = true")
    List<AnalysisTag> findPrimaryTagsByAnalysisResultId(@Param("analysisResultId") Long analysisResultId);

    /**
     * 删除分析结果的所有标签关联
     */
    void deleteByAnalysisResultId(Long analysisResultId);

    /**
     * 统计某标签被使用的次数
     */
    long countByTagId(Long tagId);

    /**
     * 查询置信度高于阈值的所有关联
     */
    @Query("SELECT at FROM AnalysisTag at WHERE at.confidence >= :minConfidence")
    List<AnalysisTag> findByConfidenceGreaterThanEqual(@Param("minConfidence") Integer minConfidence);

    /**
     * 根据合集ID查询标签详情
     * 返回: [标签名称, 父标签名称]
     */
    @Query("""
        SELECT DISTINCT t.name, parent.name
        FROM CollectionRelation cr
        JOIN AnalysisTag at ON cr.analysisResultId = at.analysisResultId
        JOIN Tag t ON at.tagId = t.id
        LEFT JOIN Tag parent ON t.parentId = parent.id
        WHERE cr.collectionId = :collectionId
        """)
    List<Object[]> findTagDetailsByCollectionId(@Param("collectionId") Long collectionId);
}
