package com.review.agent.service;

import com.review.agent.entity.pojo.CollectionRelation;
import com.review.agent.entity.pojo.KnowledgeMastery;
import com.review.agent.entity.pojo.Tag;
import com.review.agent.entity.vo.SimpleEdgeVO;
import com.review.agent.entity.vo.SimpleKnowledgeGraphVO;
import com.review.agent.entity.vo.SimpleNodeVO;
import com.review.agent.repository.CollectionRelationRepository;
import com.review.agent.repository.KnowledgeMasteryRepository;
import com.review.agent.repository.MistakeRepository;
import com.review.agent.repository.QuizQuestionRepository;
import com.review.agent.repository.TagRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识图谱服务
 */
@Slf4j
@Service
public class KnowledgeGraphService {

    @Resource
    private KnowledgeMasteryRepository knowledgeMasteryRepository;

    @Resource
    private CollectionRelationRepository collectionRelationRepository;

    @Resource
    private TagRepository tagRepository;

    @Resource
    private TagService tagService;

    @Resource
    private MistakeRepository mistakeRepository;

    @Resource
    private QuizQuestionRepository quizQuestionRepository;

    // 颜色常量
    private static final String COLOR_WEAK = "#ff4d4f";      // 薄弱 (0-40%)
    private static final String COLOR_MEDIUM = "#faad14";    // 一般 (41-70%)
    private static final String COLOR_STRONG = "#52c41a";    // 良好 (71-100%)

    // 节点数量限制
    private static final int MAX_NODES = 30;

    /**
     * 获取简化版知识图谱数据
     *
     * @param userId 用户ID
     * @return 知识图谱数据
     */
    @Transactional(readOnly = true)
    public SimpleKnowledgeGraphVO getSimpleKnowledgeGraph(Long userId) {
        SimpleKnowledgeGraphVO result = new SimpleKnowledgeGraphVO();

        // 1. 获取用户的知识点掌握度列表
        List<KnowledgeMastery> masteryList = knowledgeMasteryRepository.findByUserId(userId);

        if (masteryList.isEmpty()) {
            result.setNodes(Collections.emptyList());
            result.setEdges(Collections.emptyList());
            return result;
        }

        // 2. 构建节点列表（限制数量，按掌握度排序）
        List<SimpleNodeVO> nodes = buildNodes(masteryList, userId);
        result.setNodes(nodes);

        // 3. 构建边列表（基于共现关系）
        List<SimpleEdgeVO> edges = buildEdges(nodes);
        result.setEdges(edges);

        return result;
    }

    /**
     * 构建节点列表
     */
    private List<SimpleNodeVO> buildNodes(List<KnowledgeMastery> masteryList, Long userId) {
        // 按掌握度升序排序（薄弱优先），取前 MAX_NODES 个
        List<KnowledgeMastery> sortedList = masteryList.stream()
                .sorted(Comparator.comparing(KnowledgeMastery::getMasteryScore))
                .limit(MAX_NODES)
                .collect(Collectors.toList());

        // 获取用户的技术领域标签（用于分组）
        List<Tag> techTags = tagService.findTechDomainTags(userId);
        Map<Long, String> tagNameMap = techTags.stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));

        // 构建节点
        List<SimpleNodeVO> nodes = new ArrayList<>();
        for (KnowledgeMastery mastery : sortedList) {
            SimpleNodeVO node = new SimpleNodeVO();
            node.setId(mastery.getKnowledgePoint());
            node.setName(mastery.getKnowledgePoint());
            node.setMasteryScore(mastery.getMasteryScore());
            node.setTotalAnswered(mastery.getTotalAnswered());
            node.setMistakeCount(calculateMistakeCount(mastery.getKnowledgePoint(), userId));

            // 计算节点颜色
            node.setColor(calculateColor(mastery.getMasteryScore()));

            // 计算节点大小（基础大小 + 答题次数加权 + 错误次数加权）
            node.setSymbolSize(calculateSymbolSize(mastery.getTotalAnswered(), node.getMistakeCount()));

            // 设置分组（简单策略：根据知识点名称匹配技术标签）
            node.setGroup(determineGroup(mastery.getKnowledgePoint(), techTags));

            nodes.add(node);
        }

        return nodes;
    }

    /**
     * 计算错题次数
     */
    private Integer calculateMistakeCount(String knowledgePoint, Long userId) {
        // 查询包含该知识点的题目
        List<Long> questionIds = quizQuestionRepository.findIdsByQuestionContaining(knowledgePoint);
        if (questionIds.isEmpty()) {
            return 0;
        }

        // 统计错题次数
        Integer totalMistakeCount = mistakeRepository.sumMistakeCountByUserIdAndQuestionIdIn(userId, questionIds);
        return totalMistakeCount != null ? totalMistakeCount : 0;
    }

    /**
     * 根据掌握度计算颜色
     */
    private String calculateColor(BigDecimal masteryScore) {
        if (masteryScore == null) {
            return COLOR_WEAK;
        }
        int score = masteryScore.intValue();
        if (score >= 71) {
            return COLOR_STRONG;
        } else if (score >= 41) {
            return COLOR_MEDIUM;
        } else {
            return COLOR_WEAK;
        }
    }

    /**
     * 计算节点大小
     */
    private Integer calculateSymbolSize(Integer totalAnswered, Integer mistakeCount) {
        int baseSize = 30;
        int answeredWeight = totalAnswered != null ? totalAnswered / 2 : 0;
        int mistakeWeight = mistakeCount != null ? mistakeCount * 3 : 0;

        int size = baseSize + answeredWeight + mistakeWeight;
        return Math.min(size, 80); // 上限 80
    }

    /**
     * 确定节点分组
     */
    private String determineGroup(String knowledgePoint, List<Tag> techTags) {
        // 简单策略：如果知识点名称包含技术标签名称，则归为该组
        for (Tag tag : techTags) {
            if (knowledgePoint.toLowerCase().contains(tag.getName().toLowerCase())) {
                return tag.getName();
            }
        }
        return "其他";
    }

    /**
     * 构建边列表
     * 策略：如果两个知识点出现在同一个合集中，则建立连接
     */
    private List<SimpleEdgeVO> buildEdges(List<SimpleNodeVO> nodes) {
        if (nodes.size() < 2) {
            return Collections.emptyList();
        }

        // 获取所有知识点的 ID
        Set<String> nodeIdSet = nodes.stream()
                .map(SimpleNodeVO::getId)
                .collect(Collectors.toSet());

        // 构建边（基于知识点名称匹配合集）
        List<SimpleEdgeVO> edges = new ArrayList<>();
        Set<String> edgeKeySet = new HashSet<>(); // 去重

        // 获取所有合集关联关系
        List<CollectionRelation> allRelations = collectionRelationRepository.findAll();

        // 按合集 ID 分组
        Map<Long, List<CollectionRelation>> collectionMap = allRelations.stream()
                .collect(Collectors.groupingBy(CollectionRelation::getCollectionId));

        // 对每个合集，将其中的知识点两两连接
        for (List<CollectionRelation> relations : collectionMap.values()) {
            List<Long> analysisIds = relations.stream()
                    .map(CollectionRelation::getAnalysisResultId)
                    .distinct()
                    .collect(Collectors.toList());

            // 获取这些分析结果对应的知识点
            List<String> knowledgePointsInCollection = findKnowledgePointsByAnalysisIds(analysisIds);

            // 只保留在当前节点列表中的知识点
            List<String> validPoints = knowledgePointsInCollection.stream()
                    .filter(nodeIdSet::contains)
                    .distinct()
                    .collect(Collectors.toList());

            // 两两建立连接
            for (int i = 0; i < validPoints.size(); i++) {
                for (int j = i + 1; j < validPoints.size(); j++) {
                    String source = validPoints.get(i);
                    String target = validPoints.get(j);

                    // 生成唯一边键（排序后）
                    String edgeKey = source.compareTo(target) < 0
                            ? source + "-" + target
                            : target + "-" + source;

                    if (!edgeKeySet.contains(edgeKey)) {
                        SimpleEdgeVO edge = new SimpleEdgeVO();
                        edge.setSource(source);
                        edge.setTarget(target);
                        edges.add(edge);
                        edgeKeySet.add(edgeKey);

                        // 限制边数量，避免过于复杂
                        if (edges.size() >= MAX_NODES * 2) {
                            return edges;
                        }
                    }
                }
            }
        }

        return edges;
    }

    /**
     * 根据分析结果 ID 查找对应的知识点
     * 简化实现：从 analysis_tag 或 analysis_result 中提取
     */
    private List<String> findKnowledgePointsByAnalysisIds(List<Long> analysisIds) {
        // MVP 简化：返回空列表，让边关系更简单
        // V2 可以实现从 analysis_tag 关联到知识点
        return Collections.emptyList();
    }
}
