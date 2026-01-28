package com.review.agent.repository;

import com.review.agent.entity.pojo.UserLlmConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLlmConfigRepository extends JpaRepository<UserLlmConfig, Long> {
    List<UserLlmConfig> findByUserId(Long  userId);
}