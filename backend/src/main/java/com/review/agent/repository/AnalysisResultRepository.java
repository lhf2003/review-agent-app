package com.review.agent.repository;

import com.review.agent.AnalysisResultInfo;
import com.review.agent.entity.AnalysisResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * @param status 状态
     * @return 分析结果列表
     */
    @Query(nativeQuery = true, value = """
            select a.id as id,a.file_id as fileId ,a.problem_statement as problemStatement, a.status as status, a.created_time as createdTime, m.name as tagName 
                from analysis_result a 
                  left join analysis_tag at on a.id = at.analysis_id
                  left join main_tag m on at.tag_id = m.id
            where (:problemStatement is null or :problemStatement = '' or a.problem_statement like concat('%', :problemStatement, '%'))
            and (:tagId is null or at.tag_id = :tagId)
            and (:userId is null or a.user_id = :userId)
            and (:status is null or a.status = :status)
                        and 1= 1
            """)
    List<AnalysisResultInfo> findByPage(Pageable pageable,
                                        @Param("problemStatement") String problemStatement,
                                        @Param("tagId") Long tagId,
                                        @Param("userId") Long userId,
                                        @Param("status") Integer status);

    @Query("select a from AnalysisResult a where a.userId = :userId and a.fileId = :dataId order by a.createdTime desc limit 1")
    AnalysisResult findByUserIdAndDataId(Long userId, Long dataId);

    @Query("""
            select a from AnalysisResult a where a.userId = :userId 
                        and (:dataId is null or a.fileId = :dataId )
                        and (:analysisId is null or a.id = :analysisId )
                        order by a.createdTime desc limit 1
            """)
    AnalysisResult findByCondition(Long userId, Long dataId, Long analysisId);
}