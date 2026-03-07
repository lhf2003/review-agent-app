package com.review.agent.service;

import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.pojo.AnalysisTag;
import com.review.agent.entity.pojo.AsyncTask;
import com.review.agent.entity.pojo.Tag;
import com.review.agent.entity.pojo.TagRelation;
import com.review.agent.graph.utils.NodeRetryHelper;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.AnalysisTagRepository;
import com.review.agent.repository.TagRelationRepository;
import com.review.agent.repository.TagRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 标签关系自动发现服务
 * 基于AI分析自动识别标签之间的关系
 */
@Slf4j
@Service
public class TagRelationDiscoveryService {

    @Resource
    private TagRelationRepository tagRelationRepository;

    @Resource
    private TagRepository tagRepository;

    @Resource
    private AnalysisTagRepository analysisTagRepository;

    @Resource
    private AnalysisResultRepository analysisResultRepository;

    @Resource
    private PromptService promptService;

    @Resource(name = "analysisChatClient")
    private ChatClient chatClient;

    @Resource
    private AsyncTaskService asyncTaskService;

    @Resource
    private TagRelationService tagRelationService;

    /**
     * 为单个分析结果发现标签关系
     * 当分析结果保存后调用，分析其标签之间的潜在关系
     */
    @Transactional
    public void discoverRelationsForAnalysis(Long analysisResultId) {
        log.info("开始为分析结果 {} 发现标签关系", analysisResultId);

        // 1. 获取分析结果的所有标签
        List<AnalysisTag> analysisTags = analysisTagRepository.findByAnalysisResultId(analysisResultId);
        if (analysisTags.size() < 2) {
            log.debug("分析结果 {} 标签数量不足，无法发现关系", analysisResultId);
            return;
        }

        // 2. 获取标签详情
        List<Tag> tags = analysisTags.stream()
                .map(at -> tagRepository.findById(at.getTagId()).orElse(null))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (tags.size() < 2) {
            return;
        }

        // 3. 获取分析内容
        AnalysisResult analysisResult = analysisResultRepository.findById(analysisResultId).orElse(null);
        if (analysisResult == null) {
            return;
        }

        // 4. 分析标签之间的关系
        List<DiscoveredRelation> discoveredRelations = analyzeTagRelations(tags, analysisResult);

        // 5. 保存发现的关系
        for (DiscoveredRelation relation : discoveredRelations) {
            saveDiscoveredRelation(relation, analysisResult.getUserId());
        }

        log.info("分析结果 {} 的标签关系发现完成，发现 {} 个关系", analysisResultId, discoveredRelations.size());
    }

    /**
     * 批量发现用户的所有标签关系
     * 用于初始化或定期全量分析
     */
    @Transactional
    public void discoverAllRelationsForUser(Long userId) {
        log.info("开始为用户 {} 批量发现标签关系", userId);

        // 1. 获取用户的所有分析结果
        List<AnalysisResult> analysisResults = analysisResultRepository.findByUserId(userId);

        int totalDiscovered = 0;
        for (AnalysisResult result : analysisResults) {
            List<DiscoveredRelation> relations = discoverRelationsForTags(result.getId());
            for (DiscoveredRelation relation : relations) {
                if (saveDiscoveredRelation(relation, userId)) {
                    totalDiscovered++;
                }
            }
        }

        log.info("用户 {} 的标签关系批量发现完成，共发现 {} 个新关系", userId, totalDiscovered);
    }

    /**
     * 基于共现分析发现标签关系
     * 经常一起出现的标签可能存在关系
     */
    public List<DiscoveredRelation> discoverRelationsByCoOccurrence(Long userId) {
        log.info("基于共现分析为用户 {} 发现标签关系", userId);

        List<DiscoveredRelation> discoveredRelations = new ArrayList<>();

        // 1. 获取用户的所有分析结果及其标签
        List<AnalysisResult> analysisResults = analysisResultRepository.findByUserId(userId);

        // 2. 统计标签共现次数
        Map<String, Integer> coOccurrenceCount = new HashMap<>();
        Map<String, String> tagPairEvidence = new HashMap<>();

        for (AnalysisResult result : analysisResults) {
            List<AnalysisTag> tags = analysisTagRepository.findByAnalysisResultId(result.getId());
            List<Long> tagIds = tags.stream()
                    .map(AnalysisTag::getTagId)
                    .distinct()
                    .toList();

            // 统计两两共现
            for (int i = 0; i < tagIds.size(); i++) {
                for (int j = i + 1; j < tagIds.size(); j++) {
                    String pairKey = tagIds.get(i) + "-" + tagIds.get(j);
                    coOccurrenceCount.merge(pairKey, 1, Integer::sum);

                    // 保存示例证据（最多3个）
                    if (coOccurrenceCount.get(pairKey) <= 3) {
                        String evidence = tagPairEvidence.getOrDefault(pairKey, "") +
                                "; " + result.getProblemStatement().substring(0,
                                        Math.min(50, result.getProblemStatement().length()));
                        tagPairEvidence.put(pairKey, evidence);
                    }
                }
            }
        }

        // 3. 根据共现次数判断关系类型
        for (Map.Entry<String, Integer> entry : coOccurrenceCount.entrySet()) {
            if (entry.getValue() >= 3) { // 至少共现3次才考虑有关系
                String[] ids = entry.getKey().split("-");
                Long sourceId = Long.parseLong(ids[0]);
                Long targetId = Long.parseLong(ids[1]);

                // 获取标签信息
                Tag sourceTag = tagRepository.findById(sourceId).orElse(null);
                Tag targetTag = tagRepository.findById(targetId).orElse(null);

                if (sourceTag == null || targetTag == null) {
                    continue;
                }

                // 根据共现强度推断关系
                TagRelation.RelationType relationType = inferRelationTypeByCoOccurrence(
                        sourceTag, targetTag, entry.getValue());

                int strength = Math.min(100, 50 + entry.getValue() * 10);

                DiscoveredRelation relation = new DiscoveredRelation(
                        sourceId, targetId, relationType, strength,
                        "标签在 " + entry.getValue() + " 个知识卡片中共同出现，关联证据：" + tagPairEvidence.get(entry.getKey()),
                        true
                );

                discoveredRelations.add(relation);
            }
        }

        return discoveredRelations;
    }

    /**
     * 分析特定分析结果的标签关系
     */
    private List<DiscoveredRelation> analyzeTagRelations(List<Tag> tags, AnalysisResult analysisResult) {
        List<DiscoveredRelation> relations = new ArrayList<>();

        // 1. 基于规则发现关系
        relations.addAll(discoverRelationsByRules(tags));

        // 2. 基于AI发现关系
        relations.addAll(discoverRelationsByAI(tags, analysisResult));

        return relations;
    }

    /**
     * 基于规则发现标签关系
     */
    private List<DiscoveredRelation> discoverRelationsByRules(List<Tag> tags) {
        List<DiscoveredRelation> relations = new ArrayList<>();

        // 按维度分组
        Map<Long, List<Tag>> tagsByDimension = tags.stream()
                .collect(Collectors.groupingBy(Tag::getDimensionId));

        // 1. 父子关系（同一维度内的层级关系）
        for (Tag tag : tags) {
            if (tag.getParentId() != null) {
                // 检查父标签是否也在列表中
                boolean parentInList = tags.stream()
                        .anyMatch(t -> t.getId().equals(tag.getParentId()));
                if (parentInList) {
                    relations.add(new DiscoveredRelation(
                            tag.getParentId(), tag.getId(),
                            TagRelation.RelationType.DEPENDS_ON,
                            90,
                            "父标签是子标签的前置依赖",
                            false
                    ));
                }
            }
        }

        // 2. 思维范式间的依赖关系
        List<Tag> paradigmTags = tags.stream()
                .filter(t -> t.getParadigmCode() != null && !t.getParadigmCode().isEmpty())
                .toList();

        // 权衡分析依赖于问题分解
        Tag tradeOffTag = findParadigmTag(paradigmTags, "TRADE_OFF_ANALYSIS");
        Tag decompositionTag = findParadigmTag(paradigmTags, "DECOMPOSITION");
        if (tradeOffTag != null && decompositionTag != null) {
            relations.add(new DiscoveredRelation(
                    decompositionTag.getId(), tradeOffTag.getId(),
                    TagRelation.RelationType.DEPENDS_ON, 80,
                    "权衡分析需要先能分解问题",
                    false
            ));
        }

        // 抽象建模依赖于模式识别
        Tag abstractionTag = findParadigmTag(paradigmTags, "ABSTRACTION_MODELING");
        Tag patternTag = findParadigmTag(paradigmTags, "PATTERN_RECOGNITION");
        if (abstractionTag != null && patternTag != null) {
            relations.add(new DiscoveredRelation(
                    patternTag.getId(), abstractionTag.getId(),
                    TagRelation.RelationType.DEPENDS_ON, 75,
                    "抽象建模需要先识别模式",
                    false
            ));
        }

        return relations;
    }

    /**
     * 基于AI发现标签关系
     */
    private List<DiscoveredRelation> discoverRelationsByAI(List<Tag> tags, AnalysisResult analysisResult) {
        List<DiscoveredRelation> relations = new ArrayList<>();

        if (tags.size() < 2) {
            return relations;
        }

        try {
            // 构建提示词
            String tagsDescription = tags.stream()
                    .map(t -> String.format("- %s (维度: %s, 层级: %d)",
                            t.getName(),
                            getDimensionName(t.getDimensionId()),
                            t.getLevel()))
                    .collect(Collectors.joining("\n"));

            String prompt = String.format("""
                    分析以下知识卡片中的标签关系：

                    知识卡片内容：
                    %s

                    标签列表：
                    %s

                    请分析这些标签之间可能存在的关系，输出JSON格式：
                    {
                      "relations": [
                        {
                          "sourceTag": "源标签名称",
                          "targetTag": "目标标签名称",
                          "relationType": "DEPENDS_ON|SIMILAR_TO|COMPLEMENTS|CONFLICTS_WITH|EVOLVES_TO",
                          "strength": 75,
                          "reason": "关系判断理由"
                        }
                      ]
                    }

                    关系类型说明：
                    - DEPENDS_ON: 前置依赖，理解A后才能理解B
                    - SIMILAR_TO: 概念相似，两者思路类似
                    - COMPLEMENTS: 互补，两者结合使用效果更好
                    - CONFLICTS_WITH: 冲突，两者不能同时使用
                    - EVOLVES_TO: 演进，A是B的进化版

                    只输出JSON，不要其他说明。
                    """,
                    analysisResult.getProblemStatement().substring(0,
                            Math.min(200, analysisResult.getProblemStatement().length())),
                    tagsDescription);

            // 调用AI
            AiRelationResult result = NodeRetryHelper.builder()
                    .operation("TagRelationDiscovery[" + analysisResult.getId() + "]")
                    .chatClient(chatClient)
                    .systemPrompt("你是一个专业的知识图谱关系分析专家。")
                    .userPrompt(prompt)
                    .execute(AiRelationResult.class, () -> new AiRelationResult(Collections.emptyList()));

            if (result != null && result.relations != null) {
                for (AiRelationItem item : result.relations) {
                    // 查找标签ID
                    Tag sourceTag = findTagByName(tags, item.sourceTag);
                    Tag targetTag = findTagByName(tags, item.targetTag);

                    if (sourceTag != null && targetTag != null) {
                        relations.add(new DiscoveredRelation(
                                sourceTag.getId(), targetTag.getId(),
                                TagRelation.RelationType.valueOf(item.relationType),
                                item.strength,
                                item.reason,
                                true
                        ));
                    }
                }
            }

        } catch (Exception e) {
            log.warn("AI分析标签关系失败: {}", e.getMessage());
        }

        return relations;
    }

    /**
     * 保存发现的关系
     */
    private boolean saveDiscoveredRelation(DiscoveredRelation relation, Long userId) {
        try {
            // 检查是否已存在
            boolean exists = tagRelationRepository.existsBySourceTagIdAndTargetTagIdAndRelationType(
                    relation.sourceTagId, relation.targetTagId, relation.relationType);

            if (exists) {
                log.debug("标签关系已存在: {} -> {} ({})",
                        relation.sourceTagId, relation.targetTagId, relation.relationType);
                return false;
            }

            // 保存新关系
            TagRelation tagRelation = new TagRelation();
            tagRelation.setSourceTagId(relation.sourceTagId);
            tagRelation.setTargetTagId(relation.targetTagId);
            tagRelation.setRelationType(relation.relationType);
            tagRelation.setStrength(relation.strength);
            tagRelation.setEvidence(relation.evidence);
            tagRelation.setIsAutoDetected(relation.isAutoDetected);
            tagRelation.setUserId(userId);

            tagRelationRepository.save(tagRelation);

            log.info("保存标签关系: {} -> {} ({}) 强度={}",
                    relation.sourceTagId, relation.targetTagId,
                    relation.relationType, relation.strength);

            return true;

        } catch (Exception e) {
            log.error("保存标签关系失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 根据共现推断关系类型
     */
    private TagRelation.RelationType inferRelationTypeByCoOccurrence(Tag sourceTag, Tag targetTag, int count) {
        // 同维度标签通常是相似或互补
        if (sourceTag.getDimensionId().equals(targetTag.getDimensionId())) {
            if (count >= 5) {
                return TagRelation.RelationType.COMPLEMENTS;
            } else {
                return TagRelation.RelationType.SIMILAR_TO;
            }
        }

        // 不同维度标签可能是依赖关系
        return TagRelation.RelationType.DEPENDS_ON;
    }

    /**
     * 获取维度名称
     */
    private String getDimensionName(Long dimensionId) {
        // 简化实现，实际应从缓存或数据库获取
        return "维度" + dimensionId;
    }

    /**
     * 根据范式编码查找标签
     */
    private Tag findParadigmTag(List<Tag> tags, String paradigmCode) {
        return tags.stream()
                .filter(t -> paradigmCode.equals(t.getParadigmCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据名称查找标签
     */
    private Tag findTagByName(List<Tag> tags, String name) {
        return tags.stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 发现特定分析结果的标签关系（内部方法）
     */
    private List<DiscoveredRelation> discoverRelationsForTags(Long analysisResultId) {
        List<AnalysisTag> analysisTags = analysisTagRepository.findByAnalysisResultId(analysisResultId);

        List<Tag> tags = analysisTags.stream()
                .map(at -> tagRepository.findById(at.getTagId()).orElse(null))
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        AnalysisResult result = analysisResultRepository.findById(analysisResultId).orElse(null);
        if (result == null) {
            return Collections.emptyList();
        }

        return analyzeTagRelations(tags, result);
    }

    // ========== 异步方法 ==========

    /**
     * 异步执行基于共现的关系发现
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return CompletableFuture
     */
    @Async("defaultAsyncExecutor")
    public CompletableFuture<DiscoveryResult> discoverRelationsByCoOccurrenceAsync(Long userId, Long taskId) {
        log.info("[Task-{}] 开始异步关系发现，用户: {}", taskId, userId);

        try {
            // 更新任务状态为运行中
            asyncTaskService.updateStatus(taskId, AsyncTask.TaskStatus.RUNNING);

            // 执行发现逻辑
            List<DiscoveredRelation> relations = discoverRelationsByCoOccurrence(userId);

            // 保存发现的关系
            int savedCount = 0;
            for (int i = 0; i < relations.size(); i++) {
                DiscoveredRelation relation = relations.get(i);
                if (saveDiscoveredRelation(relation, userId)) {
                    savedCount++;
                }
                // 更新进度
                int progress = 10 + (int) ((i + 1) * 80.0 / relations.size());
                asyncTaskService.updateProgress(taskId, Math.min(90, progress));
            }

            // 构建结果
            DiscoveryResult result = new DiscoveryResult(taskId, savedCount, "SUCCESS");

            // 完成任务
            asyncTaskService.completeTask(taskId, result);

            log.info("[Task-{}] 关系发现完成，发现 {} 个新关系", taskId, savedCount);
            return CompletableFuture.completedFuture(result);

        } catch (Exception e) {
            log.error("[Task-{}] 关系发现失败: {}", taskId, e.getMessage(), e);
            asyncTaskService.failTask(taskId, e.getMessage());
            throw new RuntimeException("关系发现失败", e);
        }
    }

    // ========== 内部数据结构 ==========

    private record DiscoveredRelation(
            Long sourceTagId,
            Long targetTagId,
            TagRelation.RelationType relationType,
            int strength,
            String evidence,
            boolean isAutoDetected
    ) {}

    private record AiRelationResult(List<AiRelationItem> relations) {}

    private record AiRelationItem(
            String sourceTag,
            String targetTag,
            String relationType,
            int strength,
            String reason
    ) {}

    /**
     * 关系发现结果
     */
    public record DiscoveryResult(Long taskId, int foundCount, String status) {}
}
