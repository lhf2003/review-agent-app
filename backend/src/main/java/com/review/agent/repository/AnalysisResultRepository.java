package com.review.agent.repository;

import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.projection.AnalysisResultInfo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {
    @Query("select a from AnalysisResult a where a.userId = ?1 and a.deleted = 0")
    List<AnalysisResult> findByUserId(Long userId);

    /**
     * 软删除（设置 deleted = true 和 deleted_at = 当前时间）
     * @param id 分析结果ID
     * @param deletedAt 删除时间
     */
    @Modifying
    @Query("update AnalysisResult a set a.deleted = 1, a.deletedAt = :deletedAt where a.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 物理删除（直接删除记录，慎用）
     * @param id 分析结果ID
     */
    @Modifying
    @Query("delete from AnalysisResult a where a.id = :id")
    void hardDelete(@Param("id") Long id);

    /**
     * 分页查询分析结果（添加 deleted 过滤）
     */
    @Query(nativeQuery = true, value = """
             select a.id as id,a.file_id as fileId ,d.file_name as fileName,a.problem_statement as problemStatement, a.created_time as createTime, 
                                m.name as tagName, at.recommends as recommendTag,GROUP_CONCAT(at.sub_tag_id) as subTagIds
                 from analysis_result a
                                 left join data_info d on a.file_id = d.id
                                 left join analysis_tag at on a.id = at.analysis_id
                                 left join main_tag m on at.tag_id = m.id
                                 where (:problemStatement is null or a.problem_statement like concat('%', :problemStatement, '%'))
                                 and (:tagId is null or at.tag_id = :tagId)
                                 and (:userId is null or a.user_id = :userId)
                                 and (:fileId is null or a.file_id = :fileId )
                                 and a.deleted = 0
                                 GROUP BY a.id, at.sub_tag_id
             """)
    List<AnalysisResultInfo> findByPage(Pageable pageable,
                                          @Param("fileId") Long fileId,
                                          @Param("problemStatement") String problemStatement,
                                          @Param("tagId") Long tagId,
                                          @Param("userId") Long userId);

    @Query("select a from AnalysisResult a where a.userId = :userId and a.fileId = :dataId and a.deleted = 0 order by a.createdTime desc limit 1")
    AnalysisResult findByCondition(Long userId, Long dataId, Long analysisId);

    /**
     * 根据日期查询分析结果（添加 deleted 过滤）
     */
    @Query("select a from AnalysisResult a where a.userId = :userId and a.createdTime between :startDateTime and :endDateTime and a.deleted = 0")
    List<AnalysisResult> findAllByDate(Long userId, LocalDateTime startDateTime, LocalDateTime endDateTime);

    /**
     * 统计用户的分析结果数量（添加 deleted 过滤）
     */
    @Query("select count(a) from AnalysisResult a where a.userId = :userId and a.deleted = 0")
    long countByUserId(Long userId);

    /**
     * 根据用户ID和数据ID查询分析结果（添加 deleted 过滤）
     */
    @Query("select a from AnalysisResult a where a.userId = :userId and a.fileId = :dataId and a.deleted = 0")
    List<AnalysisResult> findByUserIdAndDataId(Long userId, Long dataId);
}