package com.review.agent.repository;

import com.review.agent.entity.pojo.QuizRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuizRecordRepository extends JpaRepository<QuizRecord, Long> {
    @Query("select q from QuizRecord q where q.userId = :userId and q.collectionId = :collectionId and q.deleted = 0 order by q.createdTime desc")
    List<QuizRecord> findByUserIdAndCollectionIdOrderByCreatedTimeDesc(Long userId, Long collectionId);

    /**
     * 统计用户指定状态的测验数量（只统计未删除的）
     */
    @Query("select count(q) from QuizRecord q where q.userId = :userId and q.status = :status and q.deleted = 0")
    long countByUserIdAndStatus(Long userId, Integer status);

    /**
     * 获取用户最近完成的2个测验（只包含未删除的）
     */
    @Query("select q from QuizRecord q where q.userId = :userId and q.status = :status and q.deleted = 0 order by q.createdTime desc limit 2")
    List<QuizRecord> findTop2ByUserIdAndStatusOrderByCreatedTimeDesc(Long userId, Integer status);

    /**
     * 软删除（设置 deleted = true 和 deleted_at = 当前时间）
     * @param id 测验记录ID
     * @param deletedAt 删除时间
     */
    @Modifying
    @Query("update QuizRecord q set q.deleted = 1, q.deletedAt = :deletedAt where q.id = :id")
    void softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 物理删除（直接删除记录，慎用）
     * @param id 测验记录ID
     */
    @Modifying
    @Query("delete from QuizRecord q where q.id = :id")
    void hardDelete(@Param("id") Long id);

    /**
     * 获取用户所有已完成的测验（按时间升序，用于趋势图）
     */
    @Query("select q from QuizRecord q where q.userId = :userId and q.status = :status and q.deleted = 0 order by q.createdTime asc")
    List<QuizRecord> findAllByUserIdAndStatusOrderByCreatedTimeAsc(Long userId, Integer status);

    /**
     * 分页查询用户的习题历史（带筛选）
     * @param userId 用户ID
     * @param status 状态筛选（null表示不筛选）
     * @param collectionId 合集筛选（null表示不筛选）
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT qr FROM QuizRecord qr " +
           "WHERE qr.userId = :userId " +
           "AND (:status IS NULL OR qr.status = :status) " +
           "AND (:collectionId IS NULL OR qr.collectionId = :collectionId) " +
           "AND qr.deleted = 0 " +
           "ORDER BY qr.createdTime DESC")
    Page<QuizRecord> findByUserIdWithFilters(
        @Param("userId") Long userId,
        @Param("status") Integer status,
        @Param("collectionId") Long collectionId,
        Pageable pageable
    );
}
