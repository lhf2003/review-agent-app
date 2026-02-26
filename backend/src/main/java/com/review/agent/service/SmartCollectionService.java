package com.review.agent.service;

import com.review.agent.entity.pojo.AchievementDefinition;
import com.review.agent.entity.pojo.AnalysisCollection;
import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.pojo.AnalysisTag;
import com.review.agent.entity.pojo.CollectionRelation;
import com.review.agent.entity.pojo.MainTag;
import com.review.agent.entity.request.QuickCreateRequest;
import com.review.agent.entity.vo.QuickCreateResultVO;
import com.review.agent.entity.vo.SmartRecommendationVO;
import com.review.agent.entity.vo.UserStatsVo;
import com.review.agent.repository.AchievementDefinitionRepository;
import com.review.agent.repository.AnalysisCollectionRepository;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.AnalysisTagRepository;
import com.review.agent.repository.CollectionRelationRepository;
import com.review.agent.repository.MainTagRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 智能合集推荐服务
 * 根据用户未归档的分析结果，按标签聚合生成合集推荐
 */
@Slf4j
@Service
public class SmartCollectionService {

    private static final String REDIS_KEY_PREFIX = "smart_recommendations:";
    private static final long CACHE_EXPIRE_HOURS = 1;
    private static final int MIN_ANALYSIS_COUNT = 3;
    private static final int MAX_RECOMMENDATIONS = 5;

    @Resource
    private AnalysisResultRepository analysisResultRepository;

    @Resource
    private AnalysisTagRepository analysisTagRepository;

    @Resource
    private MainTagRepository mainTagRepository;

    @Resource
    private CollectionRelationRepository collectionRelationRepository;

    @Resource
    private AnalysisCollectionRepository analysisCollectionRepository;

    @Resource
    private UserService userService;

    @Resource
    private AchievementDefinitionRepository achievementDefinitionRepository;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取智能推荐合集
     *
     * @param userId 用户ID
     * @return 推荐列表
     */
    public SmartRecommendationVO getSmartRecommendations(Long userId) {
        // 尝试从缓存获取
        String cacheKey = REDIS_KEY_PREFIX + userId;
        @SuppressWarnings("unchecked")
        SmartRecommendationVO cached = (SmartRecommendationVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("从缓存获取用户 {} 的智能推荐", userId);
            return cached;
        }

        // 1. 获取未归档的分析结果
        List<AnalysisResult> unarchivedResults = getUnarchivedResults(userId);

        if (unarchivedResults.isEmpty()) {
            return SmartRecommendationVO.builder()
                .recommendations(Collections.emptyList())
                .totalUnarchived(0)
                .build();
        }

        // 2. 按标签分组
        Map<Long, List<AnalysisResult>> groupedByTag = groupByMainTag(unarchivedResults);

        // 3. 生成推荐
        List<SmartRecommendationVO.RecommendationItem> recommendations = generateRecommendations(groupedByTag, userId);

        // 4. 过滤已被忽略的推荐
        @SuppressWarnings("unchecked")
        Set<String> dismissedIds = (Set<String>) redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + userId + ":dismissed");
        if (dismissedIds != null) {
            recommendations = recommendations.stream()
                .filter(r -> !dismissedIds.contains(r.getRecommendationId()))
                .collect(Collectors.toList());
        }

        // 5. 限制推荐数量
        recommendations = recommendations.stream()
            .limit(MAX_RECOMMENDATIONS)
            .collect(Collectors.toList());

        SmartRecommendationVO result = SmartRecommendationVO.builder()
            .recommendations(recommendations)
            .totalUnarchived(unarchivedResults.size())
            .build();

        // 缓存结果
        redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

        return result;
    }

    /**
     * 获取未归档的分析结果
     *
     * @param userId 用户ID
     * @return 未归档的分析结果列表
     */
    public List<AnalysisResult> getUnarchivedResults(Long userId) {
        // 获取用户所有分析结果
        List<AnalysisResult> allResults = analysisResultRepository.findByUserId(userId);

        if (allResults.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有已归档的分析结果ID
        List<Long> allResultIds = allResults.stream()
            .map(AnalysisResult::getId)
            .collect(Collectors.toList());

        // 查询这些分析结果关联的合集
        Set<Long> archivedIds = new HashSet<>();
        for (Long resultId : allResultIds) {
            List<Long> collectionIds = collectionRelationRepository.findCollectionIdsByAnalysisResultId(resultId);
            if (!collectionIds.isEmpty()) {
                archivedIds.add(resultId);
            }
        }

        // 过滤出未归档的结果
        return allResults.stream()
            .filter(r -> !archivedIds.contains(r.getId()))
            .collect(Collectors.toList());
    }

    /**
     * 按主标签分组
     *
     * @param results 分析结果列表
     * @return 按标签ID分组的Map
     */
    public Map<Long, List<AnalysisResult>> groupByMainTag(List<AnalysisResult> results) {
        if (results.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> resultIds = results.stream()
            .map(AnalysisResult::getId)
            .collect(Collectors.toList());

        // 批量获取标签关联
        List<AnalysisTag> tags = analysisTagRepository.findByAnalysisIdIn(resultIds);

        // 构建分析结果ID到结果的映射
        Map<Long, AnalysisResult> resultMap = results.stream()
            .collect(Collectors.toMap(AnalysisResult::getId, r -> r));

        // 按主标签分组
        Map<Long, List<AnalysisResult>> grouped = new HashMap<>();
        for (AnalysisTag tag : tags) {
            if (tag.getTagId() == null) {
                continue;
            }
            AnalysisResult result = resultMap.get(tag.getAnalysisId());
            if (result != null) {
                grouped.computeIfAbsent(tag.getTagId(), k -> new ArrayList<>()).add(result);
            }
        }

        return grouped;
    }

    /**
     * 生成推荐列表
     *
     * @param groupedByTag 按标签分组的分析结果
     * @param userId 用户ID
     * @return 推荐列表
     */
    private List<SmartRecommendationVO.RecommendationItem> generateRecommendations(
            Map<Long, List<AnalysisResult>> groupedByTag, Long userId) {

        List<SmartRecommendationVO.RecommendationItem> recommendations = new ArrayList<>();

        // 获取所有标签名称
        Set<Long> tagIds = groupedByTag.keySet();
        Map<Long, String> tagNameMap = new HashMap<>();
        if (!tagIds.isEmpty()) {
            List<MainTag> tags = mainTagRepository.findAllById(tagIds);
            tagNameMap = tags.stream()
                .collect(Collectors.toMap(MainTag::getId, MainTag::getName));
        }

        // 获取标签置信度
        List<Long> allResultIds = groupedByTag.values().stream()
            .flatMap(List::stream)
            .map(AnalysisResult::getId)
            .collect(Collectors.toList());

        List<AnalysisTag> allTags = analysisTagRepository.findByAnalysisIdIn(allResultIds);
        Map<Long, Double> tagAvgConfidence = calculateAverageConfidence(allTags);

        for (Map.Entry<Long, List<AnalysisResult>> entry : groupedByTag.entrySet()) {
            Long tagId = entry.getKey();
            List<AnalysisResult> results = entry.getValue();

            // 过滤数量不足的
            if (results.size() < MIN_ANALYSIS_COUNT) {
                continue;
            }

            String tagName = tagNameMap.getOrDefault(tagId, "未分类");

            // 计算置信度（基于标签置信度和数量）
            double confidence = calculateConfidence(results.size(), tagAvgConfidence.getOrDefault(tagId, 0.5));

            // 生成推荐ID
            String recommendationId = UUID.nameUUIDFromBytes((tagId + "_" + userId).getBytes()).toString();

            // 构建分析结果预览
            List<SmartRecommendationVO.AnalysisResultPreview> previews = results.stream()
                .sorted((a, b) -> b.getCreatedTime().compareTo(a.getCreatedTime()))
                .limit(10)
                .map(r -> SmartRecommendationVO.AnalysisResultPreview.builder()
                    .id(r.getId())
                    .problemStatement(truncateProblem(r.getProblemStatement(), 100))
                    .createdTime(r.getCreatedTime())
                    .build())
                .collect(Collectors.toList());

            recommendations.add(SmartRecommendationVO.RecommendationItem.builder()
                .recommendationId(recommendationId)
                .suggestedName(tagName)
                .suggestedDescription(tagName + "相关问题合集")
                .tagId(tagId)
                .tagName(tagName)
                .analysisCount(results.size())
                .analysisResults(previews)
                .confidence(confidence)
                .build());
        }

        // 按置信度和数量排序
        recommendations.sort((a, b) -> {
            // 先按数量降序
            int countCompare = Integer.compare(b.getAnalysisCount(), a.getAnalysisCount());
            if (countCompare != 0) {
                return countCompare;
            }
            // 再按置信度降序
            return Double.compare(b.getConfidence(), a.getConfidence());
        });

        return recommendations;
    }

    /**
     * 计算平均置信度
     */
    private Map<Long, Double> calculateAverageConfidence(List<AnalysisTag> tags) {
        Map<Long, List<Double>> confidenceByTag = new HashMap<>();

        for (AnalysisTag tag : tags) {
            if (tag.getTagId() != null && tag.getConfidenceScore() != null) {
                confidenceByTag.computeIfAbsent(tag.getTagId(), k -> new ArrayList<>())
                    .add(tag.getConfidenceScore());
            }
        }

        Map<Long, Double> result = new HashMap<>();
        for (Map.Entry<Long, List<Double>> entry : confidenceByTag.entrySet()) {
            double avg = entry.getValue().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.5);
            result.put(entry.getKey(), avg);
        }

        return result;
    }

    /**
     * 计算推荐置信度
     */
    private double calculateConfidence(int count, double avgConfidence) {
        // 数量因子：3个得0.6，5个得0.8，10个以上得1.0
        double countFactor = Math.min(1.0, 0.6 + (count - 3) * 0.1);

        // 综合置信度
        return Math.round(countFactor * 0.4 + avgConfidence * 0.6 * 100.0) / 100.0;
    }

    /**
     * 截断问题陈述
     */
    private String truncateProblem(String problem, int maxLength) {
        if (problem == null) {
            return "";
        }
        if (problem.length() <= maxLength) {
            return problem;
        }
        return problem.substring(0, maxLength) + "...";
    }

    /**
     * 快速创建合集
     *
     * @param userId 用户ID
     * @param request 创建请求
     * @return 创建结果
     */
    @Transactional(rollbackFor = Exception.class)
    public QuickCreateResultVO quickCreateCollection(Long userId, QuickCreateRequest request) {
        // 1. 验证分析结果归属
        List<AnalysisResult> results = analysisResultRepository.findAllById(request.getAnalysisIds());
        for (AnalysisResult result : results) {
            if (!result.getUserId().equals(userId)) {
                throw new RuntimeException("无权访问部分分析结果");
            }
        }

        // 2. 创建合集
        AnalysisCollection collection = new AnalysisCollection();
        collection.setUserId(userId);
        collection.setName(request.getName());
        collection.setDescription(request.getDescription());
        collection = analysisCollectionRepository.save(collection);

        // 3. 添加关联
        List<CollectionRelation> relations = new ArrayList<>();
        for (Long analysisId : request.getAnalysisIds()) {
            CollectionRelation relation = new CollectionRelation();
            relation.setCollectionId(collection.getId());
            relation.setAnalysisResultId(analysisId);
            relations.add(relation);
        }
        collectionRelationRepository.saveAll(relations);

        // 4. 清除推荐缓存
        clearRecommendationCache(userId);

        // 5. 触发成就检查，获取新解锁的成就
        List<String> newlyUnlockedCodes = userService.checkAndUnlockAchievements(userId);

        // 6. 获取新解锁的成就详细信息
        List<UserStatsVo.AchievementVo> newlyUnlockedAchievements = new ArrayList<>();
        if (!CollectionUtils.isEmpty(newlyUnlockedCodes)) {
            List<AchievementDefinition> definitions = achievementDefinitionRepository.findByCodeIn(newlyUnlockedCodes);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            for (AchievementDefinition definition : definitions) {
                UserStatsVo.AchievementVo vo = new UserStatsVo.AchievementVo();
                vo.setCode(definition.getCode());
                vo.setName(definition.getName());
                vo.setDescription(definition.getDescription());
                vo.setIcon(definition.getIcon());
                vo.setTarget(definition.getConditionValue());
                vo.setUnlocked(true);
                vo.setProgress(definition.getConditionValue());
                vo.setUnlockedTime(LocalDateTime.now().format(formatter));
                newlyUnlockedAchievements.add(vo);
            }
        }

        log.info("用户 {} 快速创建合集 {} 成功，包含 {} 个分析结果", userId, collection.getId(), results.size());

        return QuickCreateResultVO.builder()
            .collectionId(collection.getId())
            .createdCount(results.size())
            .newlyUnlockedAchievements(newlyUnlockedAchievements)
            .build();
    }

    /**
     * 忽略推荐
     *
     * @param userId 用户ID
     * @param recommendationId 推荐ID
     */
    @SuppressWarnings("unchecked")
    public void dismissRecommendation(Long userId, String recommendationId) {
        String key = REDIS_KEY_PREFIX + userId + ":dismissed";

        Set<String> dismissedIds = (Set<String>) redisTemplate.opsForValue().get(key);
        if (dismissedIds == null) {
            dismissedIds = new HashSet<>();
        }

        dismissedIds.add(recommendationId);
        redisTemplate.opsForValue().set(key, dismissedIds, 7, TimeUnit.DAYS);

        // 清除推荐缓存
        clearRecommendationCache(userId);

        log.info("用户 {} 忽略推荐 {}", userId, recommendationId);
    }

    /**
     * 清除推荐缓存
     */
    private void clearRecommendationCache(Long userId) {
        String cacheKey = REDIS_KEY_PREFIX + userId;
        redisTemplate.delete(cacheKey);
    }
}
