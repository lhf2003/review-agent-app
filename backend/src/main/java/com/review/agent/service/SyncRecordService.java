package com.review.agent.service;

import com.review.agent.entity.pojo.SyncRecord;
import com.review.agent.repository.SyncRecordRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SyncRecordService {
    @Resource
    private SyncRecordRepository syncRecordRepository;

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public void save(SyncRecord syncRecord) {
        syncRecord.setCreateTime(new Date());
        syncRecord.setStatus(0); // 默认状态：成功
        syncRecordRepository.save(syncRecord);
    }

    public java.util.List<SyncRecord> findAll() {
        return syncRecordRepository.findAll();
    }

    public java.util.List<SyncRecord> findByUserId(Long userId) {
        return syncRecordRepository.findByUserId(userId);
    }

    /**
     * 分页查询同步记录（支持状态、日期范围过滤）
     */
    public Page<SyncRecord> findByUserIdWithFilters(
            Long userId, Integer status, String startDate, String endDate, Pageable pageable) {
        // 先按状态查询（如果有状态过滤）
        Page<SyncRecord> page;
        if (status != null) {
            page = syncRecordRepository.findByUserIdWithStatus(userId, status, pageable);
        } else {
            page = syncRecordRepository.findByUserId(userId, pageable);
        }

        // 如果有日期范围，在内存中过滤（简化实现）
        if ((startDate != null || endDate != null) && page.hasContent()) {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            Date start = parseDate(sdf, startDate);
            Date end = parseDate(sdf, endDate);

            SimpleDateFormat dateSdf = new SimpleDateFormat(DATE_FORMAT);
            List<SyncRecord> filteredList = page.getContent().stream().filter(record -> {
                Date createTime = record.getCreateTime();
                String createTimeStr = dateSdf.format(createTime);

                if (start != null && createTime.before(start)) {
                    return false;
                }
                if (end != null && createTime.after(end)) {
                    return false;
                }
                return true;
            }).collect(Collectors.toList());

            // 创建新的 Page 对象（模拟分页结果）
            return new PageImpl<>(filteredList, pageable, filteredList.size());
        }

        return page;
    }

    /**
     * 解析日期字符串
     */
    private Date parseDate(SimpleDateFormat sdf, String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            log.warn("日期解析失败: {}", dateStr, e);
            return null;
        }
    }
}
