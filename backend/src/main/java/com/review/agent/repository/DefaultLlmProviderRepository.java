package com.review.agent.repository;

import com.review.agent.entity.pojo.DefaultLlmProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DefaultLlmProviderRepository extends JpaRepository<DefaultLlmProvider, Integer> {
}