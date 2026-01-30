package com.review.agent.repository;

import com.review.agent.entity.pojo.SyncRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncRecordRepository extends JpaRepository<SyncRecord, Long> {
    java.util.List<SyncRecord> findByUserId(Long userId);

    /**
     * 获取用户最近3条同步记录
     * @param userId 用户ID
     * @return 同步记录列表
     */
    java.util.List<SyncRecord> findTop3ByUserIdOrderByCreateTimeDesc(Long userId);
}
