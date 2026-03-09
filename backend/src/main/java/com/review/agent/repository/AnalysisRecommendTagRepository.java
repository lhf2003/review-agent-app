package com.review.agent.repository;

import com.review.agent.entity.pojo.AnalysisRecommendTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 推荐标签Repository
 */
@Repository
public interface AnalysisRecommendTagRepository extends JpaRepository<AnalysisRecommendTag, Long> {

    /**
     * 根据分析结果ID查询推荐标签
     */
    List<AnalysisRecommendTag> findByAnalysisResultId(Long analysisResultId);

    /**
     * 根据用户ID和操作状态查询推荐标签
     */
    List<AnalysisRecommendTag> findByUserIdAndUserAction(Long userId, AnalysisRecommendTag.UserAction userAction);

    /**
     * 根据用户ID查询所有推荐标签
     */
    List<AnalysisRecommendTag> findByUserIdOrderByCreatedTimeDesc(Long userId);

    /**
     * 根据用户ID查询待处理的推荐标签
     */
    default List<AnalysisRecommendTag> findPendingByUserId(Long userId) {
        return findByUserIdAndUserAction(userId, AnalysisRecommendTag.UserAction.PENDING);
    }

    /**
     * 统计用户待处理的推荐标签数量
     */
    long countByUserIdAndUserAction(Long userId, AnalysisRecommendTag.UserAction userAction);

    /**
     * 更新用户操作状态
     */
    @Modifying
    @Query("UPDATE AnalysisRecommendTag t SET t.userAction = :action WHERE t.id = :id AND t.userId = :userId")
    int updateUserAction(@Param("id") Long id,
                         @Param("userId") Long userId,
                         @Param("action") AnalysisRecommendTag.UserAction action);

    /**
     * 批量更新用户操作状态
     */
    @Modifying
    @Query("UPDATE AnalysisRecommendTag t SET t.userAction = :action WHERE t.id IN :ids AND t.userId = :userId")
    int batchUpdateUserAction(@Param("ids") List<Long> ids,
                              @Param("userId") Long userId,
                              @Param("action") AnalysisRecommendTag.UserAction action);

    /**
     * 检查是否存在指定分析结果的推荐标签
     */
    boolean existsByAnalysisResultIdAndTagName(Long analysisResultId, String tagName);

    /**
     * 查询用户最近的N条已忽略推荐标签
     */
    List<AnalysisRecommendTag> findTop5ByUserIdAndUserActionOrderByCreatedTimeDesc(
            Long userId, AnalysisRecommendTag.UserAction userAction);
}
