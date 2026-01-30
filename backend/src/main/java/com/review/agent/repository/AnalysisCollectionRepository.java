package com.review.agent.repository;

import com.review.agent.entity.pojo.AnalysisCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AnalysisCollectionRepository extends JpaRepository<AnalysisCollection, Long> {
    List<AnalysisCollection> findByUserIdOrderByCreatedTimeDesc(Long userId);

    @Query("select c from AnalysisCollection c where c.id = :collectionId and c.userId = :userId")
    AnalysisCollection findByIdAndUserId(Long userId, Long collectionId);

    /**
     * 统计用户的合集数量
     * @param userId 用户ID
     * @return 合集数量
     */
    long countByUserId(Long userId);

    /**
     * 获取用户最近创建的3个合集
     * @param userId 用户ID
     * @return 合集列表
     */
    List<AnalysisCollection> findTop3ByUserIdOrderByCreatedTimeDesc(Long userId);
}
