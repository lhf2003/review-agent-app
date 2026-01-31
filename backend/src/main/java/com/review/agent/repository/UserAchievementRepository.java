package com.review.agent.repository;

import com.review.agent.entity.pojo.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    /**
     * 根据用户ID查询所有成就记录
     * @param userId 用户ID
     * @return 用户成就列表
     */
    @Query("SELECT u FROM UserAchievement u WHERE u.userId = :userId")
    List<UserAchievement> findByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID和成就代码查询成就记录
     * @param userId 用户ID
     * @param achievementCode 成就代码
     * @return 用户成就
     */
    @Query("SELECT u FROM UserAchievement u WHERE u.userId = :userId AND u.achievementCode = :achievementCode")
    UserAchievement findByUserIdAndAchievementCode(@Param("userId") Long userId, @Param("achievementCode") String achievementCode);

    /**
     * 统计用户已解锁的成就数量
     * @param userId 用户ID
     * @return 已解锁成就数量
     */
    @Query("SELECT COUNT(u) FROM UserAchievement u WHERE u.userId = :userId AND u.unlocked = 0")
    long countByUserIdAndUnlockedTrue(@Param("userId") Long userId);
}
