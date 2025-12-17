package com.review.agent.service;

import com.review.agent.entity.pojo.SyncRecord;
import com.review.agent.repository.SyncRecordRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class SyncRecordService {
    @Resource
    private SyncRecordRepository syncRecordRepository;

    public void save(SyncRecord syncRecord) {
        syncRecord.setCreateTime(new Date());
        syncRecordRepository.save(syncRecord);
    }

    public java.util.List<SyncRecord> findAll() {
        return syncRecordRepository.findAll();
    }

    public java.util.List<SyncRecord> findByUserId(Long userId) {
        return syncRecordRepository.findByUserId(userId);
    }
}
