package com.review.agent.service;

import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.Tag;
import com.review.agent.entity.pojo.TagRelation;
import com.review.agent.entity.vo.TagRelationVO;
import com.review.agent.repository.TagRelationRepository;
import com.review.agent.repository.TagRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 标签关系服务
 * 管理标签之间的关系，提供关系查询和推荐功能
 */
@Slf4j
@Service
public class TagRelationService {

    @Resource
    private TagRelationRepository tagRelationRepository;

    @Resource
    private TagRepository tagRepository;

    @Resource
    private SecurityUtils securityUtils;

    /**
     * 创建标签关系
     */
    @Transactional
    public TagRelation createRelation(Long sourceTagId, Long targetTagId,
                                       TagRelation.RelationType relationType,
                                       Integer strength, String evidence) {
        Long userId = securityUtils.getCurrentUserId();

        // 检查是否已存在
        boolean exists = tagRelationRepository.existsBySourceTagIdAndTargetTagIdAndRelationType(
                sourceTagId, targetTagId, relationType);
        if (exists) {
            throw new IllegalArgumentException("该标签关系已存在");
        }

        // 检查标签是否存在
        if (!tagRepository.existsById(sourceTagId) || !tagRepository.existsById(targetTagId)) {
            throw new IllegalArgumentException("标签不存在");
        }

        TagRelation relation = new TagRelation();
        relation.setSourceTagId(sourceTagId);
        relation.setTargetTagId(targetTagId);
        relation.setRelationType(relationType);
        relation.setStrength(strength != null ? strength : 50);
        relation.setEvidence(evidence);
        relation.setIsAutoDetected(false);
        relation.setUserId(userId);

        return tagRelationRepository.save(relation);
    }

    /**
     * 删除标签关系
     */
    @Transactional
    public void deleteRelation(Long relationId) {
        Long userId = securityUtils.getCurrentUserId();

        TagRelation relation = tagRelationRepository.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在"));

        // 检查权限（只能删除自己创建的关系）
        if (relation.getUserId() != null && !relation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此关系");
        }

        tagRelationRepository.deleteById(relationId);
    }

    /**
     * 更新关系强度
     */
    @Transactional
    public TagRelation updateRelationStrength(Long relationId, Integer strength) {
        Long userId = securityUtils.getCurrentUserId();

        TagRelation relation = tagRelationRepository.findById(relationId)
                .orElseThrow(() -> new IllegalArgumentException("关系不存在"));

        if (relation.getUserId() != null && !relation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权修改此关系");
        }

        relation.setStrength(strength);
        return tagRelationRepository.save(relation);
    }

    /**
     * 标签关系上下文 - 一次性加载某标签的所有相关数据
     * 用于减少SQL调用次数
     */
    private static class TagRelationContext {
        final Long tagId;
        final List<TagRelation> allRelations;
        final Map<Long, Tag> tagCache;

        TagRelationContext(Long tagId, List<TagRelation> allRelations, Map<Long, Tag> tagCache) {
            this.tagId = tagId;
            this.allRelations = allRelations;
            this.tagCache = tagCache;
        }

        Tag getTag(Long tagId) {
            return tagCache.get(tagId);
        }
    }

    /**
     * 加载标签关系上下文（核心优化方法）
     * 只调用 1 次关系查询 + 1 次标签批量查询
     */
    private TagRelationContext loadContext(Long tagId) {
        // 1. 一次性查询所有相关关系
        List<TagRelation> allRelations = tagRelationRepository.findAllRelationsByTagId(tagId);

        // 2. 收集所有相关标签ID
        Set<Long> relatedTagIds = allRelations.stream()
                .flatMap(r -> Arrays.stream(new Long[]{r.getSourceTagId(), r.getTargetTagId()}))
                .filter(id -> !id.equals(tagId))
                .collect(Collectors.toSet());

        // 3. 批量查询所有相关标签（避免N+1问题）
        Map<Long, Tag> tagCache = relatedTagIds.isEmpty()
                ? Collections.emptyMap()
                : tagRepository.findAllById(relatedTagIds).stream()
                        .collect(Collectors.toMap(Tag::getId, Function.identity()));

        return new TagRelationContext(tagId, allRelations, tagCache);
    }

    /**
     * 获取标签的所有关系
     */
    public List<TagRelationVO> getTagRelations(Long tagId) {
        TagRelationContext ctx = loadContext(tagId);

        List<TagRelationVO> result = new ArrayList<>();

        for (TagRelation relation : ctx.allRelations) {
            boolean isSource = relation.getSourceTagId().equals(tagId);
            result.add(convertToVO(relation, isSource, ctx));
        }

        return result;
    }

    /**
     * 获取标签的前置依赖
     */
    public List<Map<String, Object>> getTagDependencies(Long tagId) {
        TagRelationContext ctx = loadContext(tagId);

        return ctx.allRelations.stream()
                .filter(r -> r.getRelationType() == TagRelation.RelationType.DEPENDS_ON)
                .filter(r -> r.getTargetTagId().equals(tagId)) // 当前标签是目标，说明有前置依赖
                .map(r -> {
                    Tag tag = ctx.getTag(r.getSourceTagId());
                    if (tag == null) return null;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", tag.getId());
                    map.put("name", tag.getName());
                    map.put("strength", r.getStrength());
                    return map;
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> (Integer) b.get("strength") - (Integer) a.get("strength"))
                .collect(Collectors.toList());
    }

    /**
     * 获取推荐学习的下一个标签（基于依赖关系）
     */
    public List<Map<String, Object>> getRecommendedNextTags(Long tagId) {
        TagRelationContext ctx = loadContext(tagId);

        return ctx.allRelations.stream()
                .filter(r -> r.getRelationType() == TagRelation.RelationType.DEPENDS_ON)
                .filter(r -> r.getSourceTagId().equals(tagId)) // 当前标签是源，说明被其他标签依赖
                .map(r -> {
                    Tag tag = ctx.getTag(r.getTargetTagId());
                    if (tag == null) return null;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", tag.getId());
                    map.put("name", tag.getName());
                    map.put("strength", r.getStrength());
                    return map;
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> (Integer) b.get("strength") - (Integer) a.get("strength"))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 获取相似标签
     */
    public List<Map<String, Object>> getSimilarTags(Long tagId) {
        TagRelationContext ctx = loadContext(tagId);

        return ctx.allRelations.stream()
                .filter(r -> r.getRelationType() == TagRelation.RelationType.SIMILAR_TO)
                .map(r -> {
                    Long relatedTagId = r.getSourceTagId().equals(tagId) ? r.getTargetTagId() : r.getSourceTagId();
                    Tag tag = ctx.getTag(relatedTagId);
                    if (tag == null) return null;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", tag.getId());
                    map.put("name", tag.getName());
                    map.put("strength", r.getStrength());
                    return map;
                })
                .filter(Objects::nonNull)
                .distinct()
                .sorted((a, b) -> (Integer) b.get("strength") - (Integer) a.get("strength"))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 获取互补标签
     */
    public List<Map<String, Object>> getComplementaryTags(Long tagId) {
        TagRelationContext ctx = loadContext(tagId);

        return ctx.allRelations.stream()
                .filter(r -> r.getRelationType() == TagRelation.RelationType.COMPLEMENTS)
                .map(r -> {
                    Long relatedTagId = r.getSourceTagId().equals(tagId) ? r.getTargetTagId() : r.getSourceTagId();
                    Tag tag = ctx.getTag(relatedTagId);
                    if (tag == null) return null;
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", tag.getId());
                    map.put("name", tag.getName());
                    map.put("strength", r.getStrength());
                    return map;
                })
                .filter(Objects::nonNull)
                .distinct()
                .sorted((a, b) -> (Integer) b.get("strength") - (Integer) a.get("strength"))
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户所有的标签关系
     */
    public List<TagRelationVO> getUserRelations() {
        Long userId = securityUtils.getCurrentUserId();

        List<TagRelation> relations = tagRelationRepository.findByUserId(userId);

        return relations.stream()
                .map(r -> convertToVO(r, r.getSourceTagId() != null))
                .collect(Collectors.toList());
    }

    /**
     * 获取AI自动发现的关系
     */
    public List<TagRelationVO> getAutoDetectedRelations() {
        List<TagRelation> relations = tagRelationRepository.findByIsAutoDetectedTrue();

        return relations.stream()
                .map(r -> convertToVO(r, true))
                .collect(Collectors.toList());
    }

    /**
     * 检查两个标签之间是否存在关系
     */
    public boolean hasRelation(Long tagId1, Long tagId2) {
        List<TagRelation> relations = tagRelationRepository.findRelationsBetweenTags(tagId1, tagId2);
        return !relations.isEmpty();
    }

    /**
     * 转换为VO（使用上下文缓存）
     */
    private TagRelationVO convertToVO(TagRelation relation, boolean isSource, TagRelationContext ctx) {
        TagRelationVO vo = new TagRelationVO();
        vo.setId(relation.getId());
        vo.setRelationType(relation.getRelationType());
        vo.setRelationTypeLabel(relation.getRelationType().getLabel());
        vo.setStrength(relation.getStrength());
        vo.setEvidence(relation.getEvidence());
        vo.setAutoDetected(relation.getIsAutoDetected());

        Tag sourceTag = ctx != null ? ctx.getTag(relation.getSourceTagId()) :
                tagRepository.findById(relation.getSourceTagId()).orElse(null);
        Tag targetTag = ctx != null ? ctx.getTag(relation.getTargetTagId()) :
                tagRepository.findById(relation.getTargetTagId()).orElse(null);

        if (sourceTag != null) {
            vo.setSourceTagId(sourceTag.getId());
            vo.setSourceTagName(sourceTag.getName());
        }

        if (targetTag != null) {
            vo.setTargetTagId(targetTag.getId());
            vo.setTargetTagName(targetTag.getName());
        }

        vo.setDirection(isSource ? "outgoing" : "incoming");

        return vo;
    }

    /**
     * 转换为VO（原始方法，向后兼容）
     */
    private TagRelationVO convertToVO(TagRelation relation, boolean isSource) {
        return convertToVO(relation, isSource, null);
    }
}
