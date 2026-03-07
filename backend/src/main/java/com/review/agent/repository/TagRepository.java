package com.review.agent.repository;

import com.review.agent.entity.pojo.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 标签Repository（新）
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    /**
     * 根据维度查询标签
     */
    List<Tag> findByDimensionId(Long dimensionId);

    /**
     * 根据维度和用户查询
     */
    List<Tag> findByDimensionIdAndUserId(Long dimensionId, Long userId);

    /**
     * 根据维度、层级和用户查询
     */
    List<Tag> findByDimensionIdAndLevelAndUserId(Long dimensionId, Integer level, Long userId);

    /**
     * 根据维度和层级查询（系统标签）
     */
    List<Tag> findByDimensionIdAndLevel(Long dimensionId, Integer level);

    /**
     * 查询子标签
     */
    List<Tag> findByParentIdOrderByName(Long parentId);

    /**
     * 根据范式编码查询
     */
    Optional<Tag> findByParadigmCode(String paradigmCode);

    /**
     * 根据路径前缀查询（查询某分支下所有标签）
     */
    List<Tag> findByPathStartingWith(String pathPrefix);

    /**
     * 查询某用户的所有标签
     */
    List<Tag> findByUserId(Long userId);

    /**
     * 查询所有系统内置标签
     */
    @Query("SELECT t FROM Tag t WHERE t.userId IS NULL")
    List<Tag> findSystemTags();

    /**
     * 查询某维度下的系统标签
     */
    @Query("SELECT t FROM Tag t WHERE t.dimensionId = :dimensionId AND t.userId IS NULL")
    List<Tag> findSystemTagsByDimension(@Param("dimensionId") Long dimensionId);

    /**
     * 根据名称和维度查询
     */
    Optional<Tag> findByNameAndDimensionId(String name, Long dimensionId);

    /**
     * 统计用户的标签数量
     */
    long countByUserId(Long userId);

    /**
     * 查询所有思维范式标签
     */
    @Query("SELECT t FROM Tag t WHERE t.paradigmCode IS NOT NULL AND t.paradigmCode != ''")
    List<Tag> findAllThinkingParadigms();
}
