package com.review.agent.repository;

import com.review.agent.entity.pojo.SyncRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncRecordRepository extends JpaRepository<SyncRecord, Long> {
    java.util.List<SyncRecord> findByUserId(Long userId);
}
