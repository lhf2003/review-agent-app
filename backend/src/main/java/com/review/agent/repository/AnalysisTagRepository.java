package com.review.agent.repository;

import com.review.agent.entity.pojo.AnalysisTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 通过合集ID获取标签详情（包括标签名称）
     *
     * @param collectionId 合集ID
     * @return 标签详情列表 [mainTagName, subTagName, recommends]
     */
    @Query(value = "SELECT DISTINCT mt.name, st.name, at.recommends " +
            "FROM analysis_collection ac " +
            "JOIN collection_relation cr ON ac.id = cr.collection_id " +
            "JOIN analysis_result ar ON cr.analysis_result_id = ar.id " +
            "JOIN analysis_tag at ON ar.id = at.analysis_id " +
            "LEFT JOIN main_tag mt ON at.tag_id = mt.id " +
            "LEFT JOIN sub_tag st ON FIND_IN_SET(CAST(st.id AS CHAR), at.sub_tag_id) > 0 " +
            "WHERE ac.id = :collectionId " +
            "AND ar.deleted = 0 " +
            "AND ac.deleted = 0", nativeQuery = true)
    List<Object[]> findTagDetailsByCollectionId(@Param("collectionId") Long collectionId);

}