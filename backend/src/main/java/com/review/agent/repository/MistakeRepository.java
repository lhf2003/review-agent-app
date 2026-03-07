package com.review.agent.repository;

import com.review.agent.entity.pojo.Mistake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题本 Repository
 */
public interface MistakeRepository extends JpaRepository<Mistake, Long> {

    /**
     * 获取用户所有错题
     */
    List<Mistake> findByUserId(Long userId);

    /**
     * 获取用户未掌握的错题
     */
    @Query("select m from Mistake m where m.userId = :userId and m.mastered = false order by m.lastMistakeTime desc")
    List<Mistake> findUnmasteredByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和题目ID查找错题记录
     */
    List<Mistake> findByUserIdAndQuestionId(Long userId, Long questionId);

    /**
     * 获取用户在指定时间段内的错题（用于遗忘曲线复习）
     */
    @Query("select m from Mistake m where m.userId = :userId and m.lastMistakeTime between :startTime and :endTime and m.mastered = false")
    List<Mistake> findByUserIdAndTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 获取用户错题数量（按掌握状态分组）
     */
    @Query("select count(m) from Mistake m where m.userId = :userId and m.mastered = false")
    long countUnmasteredByUserId(@Param("userId") Long userId);

    /**
     * 获取用户已掌握的错题数量
     */
    @Query("select count(m) from Mistake m where m.userId = :userId and m.mastered = true")
    long countMasteredByUserId(@Param("userId") Long userId);

    /**
     * 批量删除用户的错题（物理删除）
     */
    @Modifying
    @Query("delete from Mistake m where m.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 获取用户最近答错的题目
     */
    @Query("select m from Mistake m where m.userId = :userId order by m.lastMistakeTime desc limit 10")
    List<Mistake> findRecentMistakesByUserId(@Param("userId") Long userId);

    /**
     * 统计用户指定题目的总错误次数
     *
     * @param userId      用户ID
     * @param questionIds 题目ID列表
     * @return 总错误次数
     */
    @Query("SELECT SUM(m.mistakeCount) FROM Mistake m WHERE m.userId = :userId AND m.questionId IN :questionIds")
    Integer sumMistakeCountByUserIdAndQuestionIdIn(@Param("userId") Long userId, @Param("questionIds") List<Long> questionIds);
}
