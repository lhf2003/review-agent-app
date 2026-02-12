package com.review.agent.repository;

import com.review.agent.entity.pojo.AnalysisCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalysisCollectionRepository extends JpaRepository<AnalysisCollection, Long> {
    @Query("select c from AnalysisCollection c where c.userId = :userId and c.deleted = 0 order by c.createdTime desc")
    List<AnalysisCollection> findByUserIdOrderByCreatedTimeDesc(Long userId);

    @Query("select c from AnalysisCollection c where c.id = :collectionId and c.userId = :userId and c.deleted = 0")
    AnalysisCollection findByIdAndUserId(Long userId, Long collectionId);

    /**
     * 统计用户的合集数量（只统计未删除的）
     */
    @Query("select count(c) from AnalysisCollection c where c.userId = :userId and c.deleted = 0")
    long countByUserId(Long userId);

    /**
     * 获取用户最近创建的3个合集（只包含未删除的）
     */
    @Query("select c from AnalysisCollection c where c.userId = :userId and c.deleted = 0 order by c.createdTime desc limit 3")
    List<AnalysisCollection> findTop3ByUserIdOrderByCreatedTimeDesc(Long userId);

    /**
     * 软删除（设置 deleted = true 和 deleted_at = 当前时间）
     * @param id 合集ID
     * @param deletedAt 删除时间
     */
    @Modifying
    @Query("update AnalysisCollection c set c.deleted = 1, c.deletedAt = :deletedAt where c.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 物理删除（直接删除记录，慎用）
     * @param id 合集ID
     */
    @Modifying
    @Query("delete from AnalysisCollection c where c.id = :id")
    void hardDelete(@Param("id") Long id);

    /**
     * 通过ID列表批量查询合集
     * @param ids 合集ID列表
     * @return 合集列表
     */
    @Query("select c from AnalysisCollection c where c.id in :ids and c.deleted = 0")
    List<AnalysisCollection> findByIdIn(@Param("ids") List<Long> ids);
}
