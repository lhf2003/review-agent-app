package com.review.agent.service;

import com.review.agent.entity.vo.CollectionRecommendationVO;

import java.util.List;

/**
 * 合集推荐服务
 * 基于用户薄弱知识点智能推荐学习合集
 */
public interface CollectionRecommendationService {

    /**
     * 获取合集推荐列表
     *
     * @param userId 用户ID
     * @param limit  限制数量
     * @return 推荐列表
     */
    List<CollectionRecommendationVO> getRecommendations(Long userId, int limit);

    /**
     * 获取合集的标签列表
     *
     * @param collectionId 合集ID
     * @return 标签列表
     */
    List<String> getCollectionTags(Long collectionId);
}
