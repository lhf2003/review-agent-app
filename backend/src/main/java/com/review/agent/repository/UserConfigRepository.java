package com.review.agent.repository;

import com.review.agent.entity.UserConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConfigRepository extends JpaRepository<UserConfig, Integer> {
}