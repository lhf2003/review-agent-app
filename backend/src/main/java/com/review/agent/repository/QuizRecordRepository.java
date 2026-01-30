package com.review.agent.repository;

import com.review.agent.entity.pojo.QuizRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRecordRepository extends JpaRepository<QuizRecord, Long> {
    List<QuizRecord> findByUserIdAndCollectionIdOrderByCreatedTimeDesc(Long userId, Long collectionId);

    /**
     * 统计用户指定状态的测验数量
     * @param userId 用户ID
     * @param status 测验状态
     * @return 测验数量
     */
    long countByUserIdAndStatus(Long userId, Integer status);

    /**
     * 获取用户最近完成的2个测验
     * @param userId 用户ID
     * @param status 测验状态
     * @return 测验列表
     */
    List<QuizRecord> findTop2ByUserIdAndStatusOrderByCreatedTimeDesc(Long userId, Integer status);
}
