package com.review.agent.repository;

import com.review.agent.entity.pojo.AchievementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, Integer> {
    /**
     * 根据成就代码查询成就定义
     * @param code 成就代码
     * @return 成就定义
     */
    AchievementDefinition findByCode(String code);

    /**
     * 根据分类查询成就列表
     * @param category 成就分类
     * @return 成就列表
     */
    List<AchievementDefinition> findByCategoryOrderByOrderIndexAsc(String category);

    /**
     * 查询所有成就定义（按排序索引升序）
     * @return 成就列表
     */
    List<AchievementDefinition> findAllByOrderByOrderIndexAsc();
}
