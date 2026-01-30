package com.review.agent.repository;

import com.review.agent.entity.pojo.UserDefaultModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 用户默认模型配置 Repository
 */
public interface UserDefaultModelConfigRepository extends JpaRepository<UserDefaultModelConfig, Long> {
    /**
     * 根据用户ID和模型类型查询配置
     */
    UserDefaultModelConfig findByUserIdAndModelType(Long userId, String modelType);

    /**
     * 根据用户ID查询所有配置
     */
    List<UserDefaultModelConfig> findByUserId(Long userId);

    /**
     * 根据用户ID删除所有配置
     */
    void deleteByUserId(Long userId);
}
