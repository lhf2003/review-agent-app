package com.review.agent.repository;

import com.review.agent.entity.pojo.TagDimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 标签维度Repository
 */
@Repository
public interface TagDimensionRepository extends JpaRepository<TagDimension, Long> {

    /**
     * 根据编码查询维度
     */
    Optional<TagDimension> findByCode(String code);

    /**
     * 检查编码是否存在
     */
    boolean existsByCode(String code);
}
