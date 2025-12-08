package com.review.agent.repository;

import com.review.agent.entity.AnalysisTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnalysisTagRepository extends JpaRepository<AnalysisTag, Long> {
    /**
     * 查询主标签被使用次数
     * @param tagId 主标签id
     * @return 被使用次数
     */
    @Query("select count(at) from AnalysisTag at where at.tagId = :tagId")
    long countByTagId(Long tagId);

    /**
     * 查询子标签被使用次数
     * @param tagId 子标签id
     * @return 被使用次数
     */
    @Query("select count(at) from AnalysisTag at where at.subTagId = :tagId")
    long countBySubTagId(Long tagId);

    @Query("select at from AnalysisTag at where at.analysisId in :analysisIdList")
    List<AnalysisTag> findByAnalysisIdIn(List<Long> analysisIdList);


    @Query("""
            select ar from AnalysisTag ar where ar.analysisId in :analysisResultIdList
            """)
    List<AnalysisTag> findAllByAnalysisResultId(List<Long> analysisResultIdList);
}
