package com.review.agent.repository;

import com.review.agent.entity.pojo.KnowledgeMastery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 知识点掌握度 Repository
 */
public interface KnowledgeMasteryRepository extends JpaRepository<KnowledgeMastery, Long> {

    /**
     * 获取用户所有知识点掌握度记录
     */
    List<KnowledgeMastery> findByUserId(Long userId);

    /**
     * 根据用户ID和知识点查找掌握度记录
     */
    KnowledgeMastery findByUserIdAndKnowledgePoint(Long userId, String knowledgePoint);

    /**
     * 根据用户ID和标签ID查找掌握度记录
     */
    Optional<KnowledgeMastery> findByUserIdAndTagId(Long userId, Long tagId);

    /**
     * 获取用户掌握度最低的知识点（薄弱环节）
     */
    @Query("select km from KnowledgeMastery km where km.userId = :userId order by km.masteryScore asc")
    List<KnowledgeMastery> findWeakestByUserId(@Param("userId") Long userId);

    /**
     * 获取用户掌握度最低的N个知识点
     */
    @Query("select km from KnowledgeMastery km where km.userId = :userId order by km.masteryScore asc limit :limit")
    List<KnowledgeMastery> findWeakestByUserIdLimit(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 获取用户最近练习的知识点
     */
    @Query("select km from KnowledgeMastery km where km.userId = :userId and km.lastPracticeTime is not null order by km.lastPracticeTime desc")
    List<KnowledgeMastery> findRecentlyPracticedByUserId(@Param("userId") Long userId);

    /**
     * 统计用户的知识点数量
     */
    @Query("select count(km) from KnowledgeMastery km where km.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 获取掌握度低于阈值的知识点
     */
    @Query("select km from KnowledgeMastery km where km.userId = :userId and km.masteryScore < :threshold order by km.masteryScore asc")
    List<KnowledgeMastery> findBelowThresholdByUserId(@Param("userId") Long userId, @Param("threshold") BigDecimal threshold);

    /**
     * 根据掌握度分数范围统计数量
     */
    @Query("select count(km) from KnowledgeMastery km where km.userId = :userId and km.masteryScore >= :minScore and km.masteryScore < :maxScore")
    long countByUserIdAndMasteryScoreBetween(@Param("userId") Long userId, @Param("minScore") BigDecimal minScore, @Param("maxScore") BigDecimal maxScore);

    /**
     * 根据掌握度等级范围统计数量（整数版本）
     */
    default long countByUserIdAndMasteryLevelBetween(Long userId, int minLevel, int maxLevel) {
        return countByUserIdAndMasteryScoreBetween(userId, BigDecimal.valueOf(minLevel), BigDecimal.valueOf(maxLevel));
    }

    /**
     * 统计掌握度大于等于指定值的知识点数量
     */
    @Query("select count(km) from KnowledgeMastery km where km.userId = :userId and km.masteryScore >= :minScore")
    long countByUserIdAndMasteryScoreGreaterThanEqual(@Param("userId") Long userId, @Param("minScore") BigDecimal minScore);

    /**
     * 统计掌握度等级大于等于指定值的知识点数量（整数版本）
     */
    default long countByUserIdAndMasteryLevelGreaterThanEqual(Long userId, int minLevel) {
        return countByUserIdAndMasteryScoreGreaterThanEqual(userId, BigDecimal.valueOf(minLevel));
    }

    /**
     * 统计掌握度小于指定值的知识点数量
     */
    @Query("select count(km) from KnowledgeMastery km where km.userId = :userId and km.masteryScore < :maxScore")
    long countByUserIdAndMasteryScoreLessThan(@Param("userId") Long userId, @Param("maxScore") BigDecimal maxScore);

    /**
     * 统计掌握度等级小于指定值的知识点数量（整数版本）
     */
    default long countByUserIdAndMasteryLevelLessThan(Long userId, int maxLevel) {
        return countByUserIdAndMasteryScoreLessThan(userId, BigDecimal.valueOf(maxLevel));
    }

    /**
     * 根据用户ID和掌握度分数大于等于指定值查询
     */
    @Query("select km from KnowledgeMastery km where km.userId = :userId and km.masteryScore >= :minScore order by km.masteryScore desc")
    List<KnowledgeMastery> findByUserIdAndMasteryScoreGreaterThanEqual(@Param("userId") Long userId, @Param("minScore") BigDecimal minScore);

    /**
     * 根据用户ID和掌握度等级（整数）大于等于指定值查询
     */
    default List<KnowledgeMastery> findByUserIdAndMasteryLevelGreaterThanEqual(Long userId, int minLevel) {
        return findByUserIdAndMasteryScoreGreaterThanEqual(userId, BigDecimal.valueOf(minLevel));
    }

    /**
     * 更新知识点掌握度（插入或更新）
     */
    @Query(value = """
        INSERT INTO knowledge_mastery (user_id, knowledge_point, total_answered, correct_count, mastery_score, average_time, last_practice_time, created_time, updated_time)
        VALUES (:userId, :knowledgePoint, :totalAnswered, :correctCount, :masteryScore, :averageTime, :lastPracticeTime, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            total_answered = total_answered + :totalAnswered,
            correct_count = correct_count + :correctCount,
            mastery_score = :masteryScore,
            average_time = :averageTime,
            last_practice_time = :lastPracticeTime,
            updated_time = NOW()
        """, nativeQuery = true)
    void upsertMastery(
            @Param("userId") Long userId,
            @Param("knowledgePoint") String knowledgePoint,
            @Param("totalAnswered") Integer totalAnswered,
            @Param("correctCount") Integer correctCount,
            @Param("masteryScore") BigDecimal masteryScore,
            @Param("averageTime") Integer averageTime,
            @Param("lastPracticeTime") LocalDateTime lastPracticeTime
    );
}
