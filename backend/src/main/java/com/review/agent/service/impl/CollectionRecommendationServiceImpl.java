package com.review.agent.service.impl;

import com.review.agent.entity.pojo.AnalysisCollection;
import com.review.agent.repository.AnalysisCollectionRepository;
import com.review.agent.repository.AnalysisTagRepository;
import com.review.agent.service.CollectionRecommendationService;
import com.review.agent.service.KnowledgeMasteryService;
import com.review.agent.entity.vo.CollectionRecommendationVO;
import com.review.agent.entity.vo.KnowledgeMasteryVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 合集推荐服务实现
 * 基于用户薄弱知识点智能推荐学习合集
 */
@Service
public class CollectionRecommendationServiceImpl implements CollectionRecommendationService {

    @Resource
    private AnalysisCollectionRepository collectionRepository;

    @Resource
    private AnalysisTagRepository analysisTagRepository;

    @Resource
    private KnowledgeMasteryService knowledgeMasteryService;

    @Override
    public List<CollectionRecommendationVO> getRecommendations(Long userId, int limit) {
        // 1. 获取用户薄弱知识点
        List<KnowledgeMasteryVO> weakPoints = knowledgeMasteryService.getWeakKnowledgePoints(userId, 20);

        // 2. 获取所有合集
        List<AnalysisCollection> collections = collectionRepository
            .findByUserIdOrderByCreatedTimeDesc(userId);

        if (collections.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 计算匹配度
        List<CollectionRecommendationVO> recommendations = collections.stream()
            .map(collection -> {
                CollectionRecommendationVO vo = CollectionRecommendationVO.builder()
                    .id(collection.getId())
                    .name(collection.getName())
                    .description(collection.getDescription())
                    .count(0L)  // TODO: 后续可通过 CollectionRelationRepository 查询关联数量
                    .createdTime(collection.getCreatedTime())
                    .updatedTime(collection.getUpdatedTime())
                    .tags(getCollectionTags(collection.getId()))
                    .recommendedAt(LocalDateTime.now())
                    .build();

                // 计算匹配度
                if (!weakPoints.isEmpty()) {
                    Map<String, Object> matchResult = calculateMatch(vo.getTags(), weakPoints);
                    vo.setMatchRate((Integer) matchResult.get("rate"));
                    vo.setReason((String) matchResult.get("reason"));
                } else {
                    vo.setMatchRate(0);
                    vo.setReason("");
                }

                return vo;
            })
            .filter(vo -> vo.getMatchRate() > 0)
            .sorted((a, b) -> b.getMatchRate().compareTo(a.getMatchRate()))
            .limit(limit)
            .collect(Collectors.toList());

        return recommendations;
    }

    @Override
    public List<String> getCollectionTags(Long collectionId) {
        // 通过关联查询获取合集的标签
        List<Object[]> tagData = analysisTagRepository.findTagDetailsByCollectionId(collectionId);

        return tagData.stream()
            .map(data -> {
                String mainTag = (String) data[0];
                String subTag = (String) data[1];
                return subTag != null && !subTag.isEmpty()
                    ? mainTag + "-" + subTag
                    : mainTag;
            })
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 计算合集与薄弱知识点的匹配度
     */
    private Map<String, Object> calculateMatch(List<String> collectionTags, List<KnowledgeMasteryVO> weakPoints) {
        int matchRate = 0;
        List<String> matchedPoints = new ArrayList<>();

        for (KnowledgeMasteryVO point : weakPoints) {
            String knowledgePoint = point.getKnowledgePoint();
            double masteryRate = point.getMasteryRate() != null
                ? point.getMasteryRate()
                : 0.0;

            // 检查合集是否包含该知识点
            boolean isMatched = collectionTags.stream()
                .anyMatch(tag ->
                    tag.toLowerCase().contains(knowledgePoint.toLowerCase()) ||
                    knowledgePoint.toLowerCase().contains(tag.toLowerCase())
                );

            if (isMatched) {
                // 掌握度越低，匹配度越高
                double contribution = (100 - masteryRate) * 0.6;
                matchRate += contribution;
                matchedPoints.add(knowledgePoint);
            }
        }

        // 限制匹配度在 0-100 之间
        matchRate = Math.min(Math.toIntExact(Math.round(matchRate)), 100);

        // 生成推荐原因
        String reason = "";
        if (!matchedPoints.isEmpty()) {
            if (matchRate >= 80) {
                reason = "涵盖你的薄弱点：" + String.join("、", matchedPoints.subList(0, Math.min(2, matchedPoints.size())));
            } else if (matchRate >= 50) {
                reason = "相关知识点：" + matchedPoints.get(0);
            } else {
                reason = "建议巩固基础";
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("rate", matchRate);
        result.put("reason", reason);
        return result;
    }
}
