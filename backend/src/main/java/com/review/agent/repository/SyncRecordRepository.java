package com.review.agent.repository;

import com.review.agent.entity.pojo.SyncRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;

public interface SyncRecordRepository extends JpaRepository<SyncRecord, Long> {
    java.util.List<SyncRecord> findByUserId(Long userId);

    /**
     * 分页查询用户同步记录
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 分页结果
     */
    Page<SyncRecord> findByUserId(Long userId, Pageable pageable);

    /**
     * 分页查询用户同步记录（按状态过滤）
     * @param userId 用户ID
     * @param status 状态：0=成功, 1=同步中, 2=失败
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("SELECT sr FROM SyncRecord sr " +
            "WHERE sr.userId = :userId " +
            "AND (:status IS NULL OR sr.status = :status) " +
            "ORDER BY sr.createTime DESC")
    Page<SyncRecord> findByUserIdWithStatus(
            Long userId,
            Integer status,
            Pageable pageable
    );

    /**
     * 获取用户最近3条同步记录
     * @param userId 用户ID
     * @return 同步记录列表
     */
    java.util.List<SyncRecord> findTop3ByUserIdOrderByCreateTimeDesc(Long userId);
}
