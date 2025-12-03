package com.review.agent.repository;

import com.review.agent.entity.TagRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TagRelationRepository extends JpaRepository<TagRelation, Long> {
    List<TagRelation> findByMainTagId(Long mainTagId);

    @Query("select t from TagRelation t where t.userId = :userId")
    List<TagRelation> findByUserId(Long userId);


    /**
     * 根据用户ID和主标签ID查询子标签关系列表
     * @param userId 用户ID
     * @param mainTagId 主标签ID
     * @return 子标签列表
     */
    @Query("select t from TagRelation t where t.userId = :userId and t.mainTagId = :mainTagId")
    List<TagRelation> findAllByMainTagIdAndUserId(Long userId, Long mainTagId);

     /**
      * 根据用户ID、主标签ID和子标签ID查询关联关系
      * @param userId 用户ID
      * @param mainTagId 主标签ID
      * @param subTagId 子标签ID
      * @return 关联关系列表
      */
    @Query("select t from TagRelation t where t.userId = :userId and t.mainTagId = :mainTagId and t.subTagId = :subTagId")
    TagRelation findRelation(Long userId, Long mainTagId, Long subTagId);
}