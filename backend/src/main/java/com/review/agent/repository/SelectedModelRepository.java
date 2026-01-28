package com.review.agent.repository;

import com.review.agent.entity.pojo.SelectedModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SelectedModelRepository extends JpaRepository<SelectedModel, Integer> {
    List<SelectedModel> findByUserIdAndProviderId(Long userId, Integer providerId);

    void deleteByUserIdAndProviderIdAndModelName(Long userId, Integer providerId, String modelName);
}