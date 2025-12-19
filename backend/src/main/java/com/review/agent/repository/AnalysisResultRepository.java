package com.review.agent.repository;

import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.projection.AnalysisResultInfo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    @Query("select a from AnalysisResult a where a.userId = ?1")
    List<AnalysisResult> findByUserId(Long userId);

    /**
     * 获取用户分析标签列表
     * @param userId 用户ID
     * @return 分析标签列表
     */
    @Query("""
            select a from AnalysisResult a where a.userId = ?1
            """)
    List<AnalysisResult> getAnalysisTagList(Long userId);

    /**
     * 分页查询分析结果
     * @param pageable 分页信息
     * @param problemStatement 问题描述
     * @param tagId 标签ID
     * @param userId 用户ID
     * @return 分析结果列表
     */
    @Query(nativeQuery = true, value = """
            select a.id as id,a.file_id as fileId ,d.file_name as fileName,a.problem_statement as problemStatement, a.created_time as createTime, 
                               m.name as tagName, at.recommends as recommendTag,GROUP_CONCAT(at.sub_tag_id) as subTagIds
                from analysis_result a 
                                left join data_info d on a.file_id = d.id
                  left join analysis_tag at on a.id = at.analysis_id
                  left join main_tag m on at.tag_id = m.id
            where (:problemStatement is null or :problemStatement = '' or a.problem_statement like concat('%', :problemStatement, '%'))
            and (:tagId is null or at.tag_id = :tagId)
            and (:userId is null or a.user_id = :userId)
            and (:fileId is null or a.file_id = :fileId )
            GROUP BY a.id, at.sub_tag_id
            """)
    List<AnalysisResultInfo> findByPage(Pageable pageable,
                                        @Param("fileId") Long fileId,
                                        @Param("problemStatement") String problemStatement,
                                        @Param("tagId") Long tagId,
                                        @Param("userId") Long userId);

    @Query("select a from AnalysisResult a where a.userId = :userId and a.fileId = :dataId order by a.createdTime desc")
    List<AnalysisResult> findByUserIdAndDataId(Long userId, Long dataId);

    @Query("""
            select a from AnalysisResult a where a.userId = :userId 
                        and (:dataId is null or a.fileId = :dataId )
                        and (:analysisId is null or a.id = :analysisId )
                        order by a.createdTime desc limit 1
            """)
    AnalysisResult findByCondition(Long userId, Long dataId, Long analysisId);

    /**
     * 根据日期查询分析结果
     * @param userId 用户ID
     * @param startDateTime 开始日期
     * @param endDateTime 结束日期
     * @return 分析结果列表
     */
    @Query("select a from AnalysisResult a where a.userId = :userId and a.createdTime between :startDateTime and :endDateTime")
    List<AnalysisResult> findAllByDate(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);
}