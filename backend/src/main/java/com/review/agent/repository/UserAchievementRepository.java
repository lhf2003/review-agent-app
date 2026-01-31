package com.review.agent.repository;

import com.review.agent.entity.pojo.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    /**
     * 根据用户ID查询所有成就记录
     * @param userId 用户ID
     * @return 用户成就列表
     */
    List<UserAchievement> findByUserId(Long userId);

    /**
     * 根据用户ID和成就代码查询成就记录
     * @param userId 用户ID
     * @param achievementCode 成就代码
     * @return 用户成就
     */
    UserAchievement findByUserIdAndAchievementCode(Long userId, String achievementCode);

    /**
     * 统计用户已解锁的成就数量
     * @param userId 用户ID
     * @return 已解锁成就数量
     */
    long countByUserIdAndUnlockedTrue(Long userId);
}
