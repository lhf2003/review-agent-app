package com.review.agent.service;

import com.review.agent.entity.pojo.*;
import com.review.agent.entity.vo.*;
import com.review.agent.repository.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 思维范式服务
 */
@Slf4j
@Service
public class ThinkingParadigmService {

    @Resource
    private TagRepository tagRepository;

    @Resource
    private TagDimensionRepository tagDimensionRepository;

    @Resource
    private TagRelationRepository tagRelationRepository;

    @Resource
    private AnalysisTagRepository analysisTagRepository;

    @Resource
    private AnalysisResultRepository analysisResultRepository;

    /**
     * 获取所有标签维度
     */
    public List<TagDimensionVO> getAllDimensions() {
        List<TagDimension> dimensions = tagDimensionRepository.findAll();
        return dimensions.stream()
                .sorted(Comparator.comparing(TagDimension::getSortOrder))
                .map(this::convertToDimensionVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有思维范式标签
     */
    public List<TagVO> getAllThinkingParadigms() {
        List<Tag> paradigms = tagRepository.findAllThinkingParadigms();
        return paradigms.stream()
                .map(this::convertToTagVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取分析结果的思维范式
     */
    public List<ThinkingParadigmVO> getParadigmsByAnalysisResult(Long userId, Long analysisResultId) {
        // 验证分析结果归属
        AnalysisResult result = analysisResultRepository.findById(analysisResultId).orElse(null);
        if (result == null || !result.getUserId().equals(userId)) {
            return Collections.emptyList();
        }

        // 查询关联的思维范式标签
        List<AnalysisTag> tagRelations = analysisTagRepository.findByAnalysisResultId(analysisResultId);
        if (CollectionUtils.isEmpty(tagRelations)) {
            return Collections.emptyList();
        }

        List<Long> tagIds = tagRelations.stream()
                .map(AnalysisTag::getTagId)
                .collect(Collectors.toList());

        List<Tag> tags = tagRepository.findAllById(tagIds);

        return tags.stream()
                .filter(tag -> tag.getParadigmCode() != null)
                .map(tag -> convertToParadigmVO(tag, tagRelations))
                .collect(Collectors.toList());
    }

    /**
     * 获取学习路径推荐
     * 基于思维范式依赖关系
     */
    public List<TagVO> getLearningPath(Long userId, Long targetParadigmId) {
        if (targetParadigmId == null) {
            // 返回所有思维范式作为学习路径
            return getAllThinkingParadigms();
        }

        // 获取目标范式的前置依赖
        List<TagRelation> dependencies = tagRelationRepository
                .findByTargetTagIdAndRelationType(targetParadigmId, TagRelation.RelationType.DEPENDS_ON);

        List<Long> dependencyIds = dependencies.stream()
                .map(TagRelation::getSourceTagId)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(dependencyIds)) {
            return Collections.singletonList(convertToTagVO(tagRepository.findById(targetParadigmId).orElse(null)));
        }

        // 构建学习路径：前置依赖 -> 目标范式
        List<Tag> pathTags = tagRepository.findAllById(dependencyIds);
        Tag targetTag = tagRepository.findById(targetParadigmId).orElse(null);

        List<TagVO> learningPath = new ArrayList<>();
        learningPath.addAll(pathTags.stream().map(this::convertToTagVO).collect(Collectors.toList()));
        if (targetTag != null) {
            learningPath.add(convertToTagVO(targetTag));
        }

        return learningPath;
    }

    /**
     * 获取相似思维范式
     */
    public List<TagVO> getSimilarParadigms(Long userId, Long paradigmId) {
        // 查询相似关系
        List<TagRelation> similarRelations = tagRelationRepository
                .findBySourceTagId(paradigmId).stream()
                .filter(r -> r.getRelationType() == TagRelation.RelationType.SIMILAR_TO)
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(similarRelations)) {
            // 如果没有预设关系，返回同一维度的其他标签
            Tag tag = tagRepository.findById(paradigmId).orElse(null);
            if (tag == null) {
                return Collections.emptyList();
            }
            return tagRepository.findByDimensionId(tag.getDimensionId()).stream()
                    .filter(t -> !t.getId().equals(paradigmId))
                    .map(this::convertToTagVO)
                    .collect(Collectors.toList());
        }

        List<Long> similarIds = similarRelations.stream()
                .map(TagRelation::getTargetTagId)
                .collect(Collectors.toList());

        return tagRepository.findAllById(similarIds).stream()
                .map(this::convertToTagVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户思维范式统计
     */
    public Map<String, Object> getUserParadigmStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 获取用户所有分析结果
        List<AnalysisResult> results = analysisResultRepository.findByUserId(userId);
        List<Long> analysisIds = results.stream().map(AnalysisResult::getId).collect(Collectors.toList());

        // 统计各思维范式的使用次数
        Map<String, Long> paradigmCount = new HashMap<>();
        for (Long analysisId : analysisIds) {
            List<AnalysisTag> tags = analysisTagRepository.findByAnalysisResultId(analysisId);
            for (AnalysisTag tag : tags) {
                Tag t = tagRepository.findById(tag.getTagId()).orElse(null);
                if (t != null && t.getParadigmCode() != null) {
                    paradigmCount.merge(t.getName(), 1L, Long::sum);
                }
            }
        }

        // 获取最常用思维范式
        List<Map<String, Object>> topParadigms = paradigmCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", e.getKey());
                    map.put("count", e.getValue());
                    return map;
                })
                .collect(Collectors.toList());

        stats.put("totalParadigms", paradigmCount.size());
        stats.put("topParadigms", topParadigms);
        stats.put("paradigmDistribution", paradigmCount);

        return stats;
    }

    // ========== 转换方法 ==========

    private TagDimensionVO convertToDimensionVO(TagDimension dimension) {
        TagDimensionVO vo = new TagDimensionVO();
        BeanUtils.copyProperties(dimension, vo);
        return vo;
    }

    private TagVO convertToTagVO(Tag tag) {
        if (tag == null) {
            return null;
        }
        TagVO vo = new TagVO();
        BeanUtils.copyProperties(tag, vo);
        vo.setIsSystemTag(tag.getUserId() == null);

        // 获取维度名称
        if (tag.getDimension() != null) {
            vo.setDimensionName(tag.getDimension().getName());
        } else {
            tagDimensionRepository.findById(tag.getDimensionId())
                    .ifPresent(d -> vo.setDimensionName(d.getName()));
        }

        // 获取父标签名称
        if (tag.getParent() != null) {
            vo.setParentName(tag.getParent().getName());
        } else if (tag.getParentId() != null) {
            tagRepository.findById(tag.getParentId())
                    .ifPresent(p -> vo.setParentName(p.getName()));
        }

        return vo;
    }

    private ThinkingParadigmVO convertToParadigmVO(Tag tag, List<AnalysisTag> tagRelations) {
        ThinkingParadigmVO vo = new ThinkingParadigmVO();
        vo.setId(tag.getId());
        vo.setCode(tag.getParadigmCode());
        vo.setName(tag.getName());
        vo.setDescription(tag.getDescription());
        vo.setWhenToUse(tag.getWhenToUse());
        vo.setExample(tag.getExample());

        // 从关联中提取置信度
        tagRelations.stream()
                .filter(t -> t.getTagId().equals(tag.getId()))
                .findFirst()
                .ifPresent(t -> vo.setConfidence(t.getConfidence()));

        return vo;
    }
}
