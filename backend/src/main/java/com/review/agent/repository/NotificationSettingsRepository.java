package com.review.agent.repository;

import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.pojo.NotificationSettings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 通知设置 Repository
 */
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    /**
     * 根据用户ID获取通知设置
     *
     * @param userId 用户ID
     * @return 通知设置
     */
    Optional<NotificationSettings> findByUserId(Long userId);

    /**
     * 检查用户是否有通知设置
     *
     * @param userId 用户ID
     * @return 是否存在
     */
    @Query("select count(ns) > 0 from NotificationSettings ns where ns.userId = :userId")
    boolean existsByUserId(@Param("userId") Long userId);
}
