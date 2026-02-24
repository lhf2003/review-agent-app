package com.review.agent.repository;

import com.review.agent.entity.pojo.MistakeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 错题答题历史Repository
 */
@Repository
public interface MistakeHistoryRepository extends JpaRepository<MistakeHistory, Long> {

    /**
     * 根据错题ID查询历史记录（按时间倒序）
     */
    List<MistakeHistory> findByMistakeIdOrderByCreatedTimeDesc(Long mistakeId);

    /**
     * 根据用户ID和题目ID查询历史记录
     */
    List<MistakeHistory> findByUserIdAndQuestionIdOrderByCreatedTimeDesc(Long userId, Long questionId);
}
