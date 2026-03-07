package com.review.agent.service;

import com.review.agent.common.utils.ExceptionUtils;
import com.review.agent.common.utils.ObjectTransformUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.*;
import com.review.agent.entity.request.TagRecommendRequest;
import com.review.agent.repository.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 标签服务（重构版）
 * 使用新的Tag实体，支持多维度标签体系
 */
@Slf4j
@Service
public class TagService {

    @Resource
    private TagRepository tagRepository;

    @Resource
    private TagDimensionRepository tagDimensionRepository;

    @Resource
    private AnalysisTagRepository analysisTagRepository;

    @Resource
    private SecurityUtils securityUtils;

    // ========== 技术领域标签（原MainTag功能） ==========

    /**
     * 查询用户的技术领域标签（一级）
     */
    public List<Tag> findTechDomainTags(Long userId) {
        TagDimension techDomain = tagDimensionRepository.findByCode("TECH_DOMAIN")
                .orElseThrow(() -> new IllegalStateException("技术领域维度不存在"));
        return tagRepository.findByDimensionIdAndLevelAndUserId(techDomain.getId(), 1, userId);
    }

    /**
     * 查询技术领域下的子标签
     */
    public List<Tag> findSubTagsByParentId(Long userId, Long parentId) {
        return tagRepository.findByParentIdOrderByName(parentId);
    }

    /**
     * 添加标签
     */
    @Transactional
    public Long addTag(Tag tag) {
        Long userId = securityUtils.getCurrentUserId();

        if (tag.getName() == null || tag.getName().isEmpty()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }

        // 设置默认值
        tag.setId(null);
        tag.setUserId(userId);
        tag.setCreatedTime(LocalDateTime.now());
        tag.setUpdatedTime(LocalDateTime.now());

        if (tag.getLevel() == null) {
            tag.setLevel(1);
        }

        // 构建路径
        if (tag.getParentId() != null) {
            Tag parent = tagRepository.findById(tag.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("父标签不存在"));
            tag.setLevel(parent.getLevel() + 1);
            tag.setPath(parent.getPath() + "/" + tag.getName());
        } else {
            tag.setPath("/" + tag.getName());
        }

        // 检查名称是否已存在（同一维度下）
        Optional<Tag> existing = tagRepository.findByNameAndDimensionId(tag.getName(), tag.getDimensionId());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("标签名称已存在");
        }

        Tag saved = tagRepository.save(tag);
        return saved.getId();
    }

    @Transactional
    public void updateTag(Tag tag) {
        Long userId = securityUtils.getCurrentUserId();

        Tag tagInDb = tagRepository.findById(tag.getId())
                .orElseThrow(() -> new IllegalArgumentException("标签不存在"));

        // 检查权限
        if (tagInDb.getUserId() != null && !tagInDb.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此标签");
        }

        BeanUtils.copyProperties(tag, tagInDb, ObjectTransformUtil.getNullPropertyNames(tag));
        tagInDb.setUpdatedTime(LocalDateTime.now());

        tagRepository.save(tagInDb);
    }

    @Transactional
    public void deleteTag(Long id) {
        Long userId = securityUtils.getCurrentUserId();

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("标签不存在"));

        // 检查权限
        if (tag.getUserId() != null && !tag.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此标签");
        }

        // 检查是否被使用
        long refCount = analysisTagRepository.countByTagId(id);
        if (refCount > 0) {
            ExceptionUtils.throwDataInUse("标签");
        }

        // 递归删除子标签
        deleteChildTags(id);

        tagRepository.deleteById(id);
    }

    private void deleteChildTags(Long parentId) {
        List<Tag> children = tagRepository.findByParentIdOrderByName(parentId);
        for (Tag child : children) {
            deleteChildTags(child.getId());
            tagRepository.deleteById(child.getId());
        }
    }

    public List<Tag> findByIdList(List<Long> tagIdList) {
        return tagRepository.findAllById(tagIdList);
    }

    // ========== 推荐标签处理 ==========

    @Transactional
    public void addRecommendTag(Long userId, TagRecommendRequest request) {
        List<AnalysisTag> analysisTagList = analysisTagRepository
                .findByAnalysisResultId(request.getAnalysisId());

        if (analysisTagList.isEmpty()) {
            // 创建新的关联
            AnalysisTag newTag = new AnalysisTag();
            newTag.setAnalysisResultId(request.getAnalysisId());
            newTag.setIsPrimary(true);
            newTag.setConfidence(100);
            analysisTagList = Collections.singletonList(newTag);
        }

        AnalysisTag analysisTag = analysisTagList.get(0);
        String name = request.getName();

        // 查找或创建标签
        TagDimension techDomain = tagDimensionRepository.findByCode("TECH_DOMAIN")
                .orElseThrow(() -> new IllegalStateException("技术领域维度不存在"));

        Tag tag = tagRepository.findByNameAndDimensionId(name, techDomain.getId())
                .orElseGet(() -> {
                    // 创建新标签
                    Tag newTag = new Tag();
                    newTag.setName(name);
                    newTag.setDimensionId(techDomain.getId());
                    newTag.setLevel(1);
                    newTag.setPath("/" + name);
                    newTag.setUserId(userId);
                    return tagRepository.save(newTag);
                });

        analysisTag.setTagId(tag.getId());
        analysisTagRepository.save(analysisTag);
    }

    // ========== 思维范式标签 ==========

    /**
     * 查询思维范式标签
     */
    public List<Tag> findThinkingParadigmTags() {
        TagDimension paradigmDimension = tagDimensionRepository.findByCode("THINKING_PARADIGM")
                .orElseThrow(() -> new IllegalStateException("思维范式维度不存在"));
        return tagRepository.findByDimensionIdAndLevel(paradigmDimension.getId(), 1);
    }

    /**
     * 根据范式编码查询标签
     */
    public Optional<Tag> findTagByParadigmCode(String paradigmCode) {
        return tagRepository.findByParadigmCode(paradigmCode);
    }

    // ========== 维度查询 ==========

    /**
     * 查询所有标签维度
     */
    public List<TagDimension> findAllDimensions() {
        return tagDimensionRepository.findAll();
    }

    // ========== 查询方法（兼容旧接口） ==========

    /**
     * 查询用户所有标签（兼容旧接口）
     */
    public List<Tag> findAllTagsByUser(Long userId) {
        return tagRepository.findByUserId(userId);
    }

    /**
     * 获取标签树结构
     */
    public List<TagVO> getTagTree(Long userId, Long dimensionId) {
        List<Tag> tags;
        if (dimensionId != null) {
            tags = tagRepository.findByDimensionIdAndUserId(dimensionId, userId);
        } else {
            tags = tagRepository.findByUserId(userId);
        }

        // 构建树形结构
        Map<Long, TagVO> voMap = new HashMap<>();
        List<TagVO> rootTags = new ArrayList<>();

        // 先创建所有VO
        for (Tag tag : tags) {
            TagVO vo = convertToVO(tag);
            voMap.put(tag.getId(), vo);
        }

        // 构建父子关系
        for (Tag tag : tags) {
            TagVO vo = voMap.get(tag.getId());
            if (tag.getParentId() == null) {
                rootTags.add(vo);
            } else {
                TagVO parent = voMap.get(tag.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                }
            }
        }

        return rootTags;
    }

    private TagVO convertToVO(Tag tag) {
        TagVO vo = new TagVO();
        BeanUtils.copyProperties(tag, vo);
        vo.setIsSystemTag(tag.getUserId() == null);

        tagDimensionRepository.findById(tag.getDimensionId())
                .ifPresent(d -> vo.setDimensionName(d.getName()));

        return vo;
    }

    // 内部VO类
    public static class TagVO {
        private Long id;
        private String name;
        private Long dimensionId;
        private String dimensionName;
        private Long parentId;
        private Integer level;
        private String path;
        private Boolean isSystemTag;
        private LocalDateTime createdTime;
        private List<TagVO> children;

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getDimensionId() { return dimensionId; }
        public void setDimensionId(Long dimensionId) { this.dimensionId = dimensionId; }
        public String getDimensionName() { return dimensionName; }
        public void setDimensionName(String dimensionName) { this.dimensionName = dimensionName; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public Integer getLevel() { return level; }
        public void setLevel(Integer level) { this.level = level; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public Boolean getIsSystemTag() { return isSystemTag; }
        public void setIsSystemTag(Boolean isSystemTag) { this.isSystemTag = isSystemTag; }
        public LocalDateTime getCreatedTime() { return createdTime; }
        public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
        public List<TagVO> getChildren() { return children; }
        public void setChildren(List<TagVO> children) { this.children = children; }
    }
}
