package com.review.agent.repository;

import com.review.agent.entity.pojo.UserConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserConfigRepository extends JpaRepository<UserConfig, Integer> {
    @Query("select u from UserConfig u where u.userId = ?1")
    UserConfig findByUserId(Long userId);
}