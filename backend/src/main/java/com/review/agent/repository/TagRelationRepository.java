package com.review.agent.repository;

import com.review.agent.entity.pojo.TagRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 标签关系Repository（新）
 */
@Repository
public interface TagRelationRepository extends JpaRepository<TagRelation, Long> {

    /**
     * 查询某标签作为源的所有关系
     */
    List<TagRelation> findBySourceTagId(Long sourceTagId);

    /**
     * 查询某标签作为目标的所有关系
     */
    List<TagRelation> findByTargetTagId(Long targetTagId);

    /**
     * 查询某标签的所有前置依赖（目标标签为当前标签，关系为DEPENDS_ON）
     */
    @Query("SELECT r FROM TagRelation r WHERE r.targetTagId = :tagId AND r.relationType = 'DEPENDS_ON'")
    List<TagRelation> findDependenciesByTargetTagId(@Param("tagId") Long tagId);

    /**
     * 查询某标签被哪些标签依赖（源标签为当前标签，关系为DEPENDS_ON）
     */
    @Query("SELECT r FROM TagRelation r WHERE r.sourceTagId = :tagId AND r.relationType = 'DEPENDS_ON'")
    List<TagRelation> findDependentsBySourceTagId(@Param("tagId") Long tagId);

    /**
     * 根据关系类型查询
     */
    List<TagRelation> findByRelationType(TagRelation.RelationType relationType);

    /**
     * 根据关系类型和用户查询
     */
    List<TagRelation> findByRelationTypeAndUserId(TagRelation.RelationType relationType, Long userId);

    /**
     * 查询两个标签之间的所有关系
     */
    @Query("SELECT r FROM TagRelation r WHERE (r.sourceTagId = :tagId1 AND r.targetTagId = :tagId2) OR (r.sourceTagId = :tagId2 AND r.targetTagId = :tagId1)")
    List<TagRelation> findRelationsBetweenTags(@Param("tagId1") Long tagId1, @Param("tagId2") Long tagId2);

    /**
     * 检查关系是否已存在
     */
    boolean existsBySourceTagIdAndTargetTagIdAndRelationType(Long sourceTagId, Long targetTagId, TagRelation.RelationType relationType);

    /**
     * 查询AI自动发现的关系
     */
    List<TagRelation> findByIsAutoDetectedTrue();

    /**
     * 根据用户查询关系
     */
    List<TagRelation> findByUserId(Long userId);

    /**
     * 查询某标签的所有特定类型关系（作为目标）
     */
    List<TagRelation> findByTargetTagIdAndRelationType(Long targetTagId, TagRelation.RelationType relationType);

    /**
     * 一次性查询与某标签相关的所有关系（作为源或目标）
     */
    @Query("SELECT r FROM TagRelation r WHERE r.sourceTagId = :tagId OR r.targetTagId = :tagId")
    List<TagRelation> findAllRelationsByTagId(@Param("tagId") Long tagId);
}
