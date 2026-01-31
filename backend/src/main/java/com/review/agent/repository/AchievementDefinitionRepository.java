package com.review.agent.repository;

import com.review.agent.entity.pojo.AchievementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, Integer> {
    /**
     * 根据成就代码查询成就定义
     * @param code 成就代码
     * @return 成就定义
     */
    @Query("SELECT a FROM AchievementDefinition a WHERE a.code = :code")
    AchievementDefinition findByCode(@Param("code") String code);

    /**
     * 根据分类查询成就列表
     * @param category 成就分类
     * @return 成就列表
     */
    @Query("SELECT a FROM AchievementDefinition a WHERE a.category = :category ORDER BY a.orderIndex ASC")
    List<AchievementDefinition> findByCategoryOrderByOrderIndexAsc(@Param("category") String category);

    /**
     * 查询所有成就定义（按排序索引升序）
     * @return 成就列表
     */
    @Query("SELECT a FROM AchievementDefinition a ORDER BY a.orderIndex ASC")
    List<AchievementDefinition> findAllByOrderByOrderIndexAsc();

    /**
     * 根据成就代码列表批量查询
     * @param codes 成就代码列表
     * @return 成就列表
     */
    @Query("SELECT a FROM AchievementDefinition a WHERE a.code IN :codes")
    List<AchievementDefinition> findByCodeIn(@Param("codes") List<String> codes);
}
