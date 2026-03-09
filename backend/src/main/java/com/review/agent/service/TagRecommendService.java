package com.review.agent.service;

import com.review.agent.entity.pojo.*;
import com.review.agent.entity.vo.RecommendTagVO;
import com.review.agent.repository.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐标签服务
 * 管理AI分析生成的推荐标签的查询、采纳、忽略等操作
 */
@Slf4j
@Service
public class TagRecommendService {

    @Resource
    private AnalysisRecommendTagRepository analysisRecommendTagRepository;

    @Resource
    private AnalysisResultRepository analysisResultRepository;

    @Resource
    private DataInfoRepository dataInfoRepository;

    @Resource
    private TagRepository tagRepository;

    @Resource
    private TagDimensionRepository tagDimensionRepository;

    @Resource
    private AnalysisTagRepository analysisTagRepository;

    /**
     * 获取推荐标签列表
     *
     * @param userId     用户ID
     * @param userAction 操作状态过滤，null则返回全部
     * @return 推荐标签VO列表
     */
    public List<RecommendTagVO> getRecommendations(Long userId, AnalysisRecommendTag.UserAction userAction) {
        List<AnalysisRecommendTag> recommendTags;

        if (userAction != null) {
            recommendTags = analysisRecommendTagRepository.findByUserIdAndUserAction(userId, userAction);
        } else {
            recommendTags = analysisRecommendTagRepository.findByUserIdOrderByCreatedTimeDesc(userId);
        }

        if (CollectionUtils.isEmpty(recommendTags)) {
            return Collections.emptyList();
        }

        // 批量查询关联的分析结果和文件信息
        Set<Long> analysisResultIds = recommendTags.stream()
                .map(AnalysisRecommendTag::getAnalysisResultId)
                .collect(Collectors.toSet());

        Map<Long, AnalysisResult> analysisResultMap = analysisResultRepository.findAllById(analysisResultIds)
                .stream()
                .collect(Collectors.toMap(AnalysisResult::getId, ar -> ar));

        Set<Long> fileIds = analysisResultMap.values().stream()
                .map(AnalysisResult::getFileId)
                .collect(Collectors.toSet());

        Map<Long, DataInfo> dataInfoMap = dataInfoRepository.findAllById(fileIds)
                .stream()
                .collect(Collectors.toMap(DataInfo::getId, di -> di));

        // 构建VO
        return recommendTags.stream()
                .map(tag -> convertToVO(tag, analysisResultMap, dataInfoMap))
                .collect(Collectors.toList());
    }

    /**
     * 获取按状态分组的推荐标签列表
     * IGNORED状态只返回最近的5条
     *
     * @param userId 用户ID
     * @return 按状态分组的推荐标签Map，key: PENDING/ADOPTED/IGNORED, value: 对应状态的VO列表
     */
    public Map<String, List<RecommendTagVO>> getRecommendationsGroupedByStatus(Long userId) {
        Map<String, List<RecommendTagVO>> result = new HashMap<>();

        // 获取PENDING状态（全部）
        List<AnalysisRecommendTag> pendingTags = analysisRecommendTagRepository
                .findByUserIdAndUserAction(userId, AnalysisRecommendTag.UserAction.PENDING);
        result.put("PENDING", convertToVOList(pendingTags));

        // 获取ADOPTED状态（全部）
        List<AnalysisRecommendTag> adoptedTags = analysisRecommendTagRepository
                .findByUserIdAndUserAction(userId, AnalysisRecommendTag.UserAction.ADOPTED);
        result.put("ADOPTED", convertToVOList(adoptedTags));

        // 获取IGNORED状态（只返回最近5条）
        List<AnalysisRecommendTag> ignoredTags = analysisRecommendTagRepository
                .findTop5ByUserIdAndUserActionOrderByCreatedTimeDesc(userId, AnalysisRecommendTag.UserAction.IGNORED);
        result.put("IGNORED", convertToVOList(ignoredTags));

        return result;
    }

    /**
     * 将推荐标签列表转换为VO列表
     */
    private List<RecommendTagVO> convertToVOList(List<AnalysisRecommendTag> recommendTags) {
        if (CollectionUtils.isEmpty(recommendTags)) {
            return Collections.emptyList();
        }

        // 批量查询关联的分析结果和文件信息
        Set<Long> analysisResultIds = recommendTags.stream()
                .map(AnalysisRecommendTag::getAnalysisResultId)
                .collect(Collectors.toSet());

        Map<Long, AnalysisResult> analysisResultMap = analysisResultRepository.findAllById(analysisResultIds)
                .stream()
                .collect(Collectors.toMap(AnalysisResult::getId, ar -> ar));

        Set<Long> fileIds = analysisResultMap.values().stream()
                .map(AnalysisResult::getFileId)
                .collect(Collectors.toSet());

        Map<Long, DataInfo> dataInfoMap = dataInfoRepository.findAllById(fileIds)
                .stream()
                .collect(Collectors.toMap(DataInfo::getId, di -> di));

        // 构建VO列表
        return recommendTags.stream()
                .map(tag -> convertToVO(tag, analysisResultMap, dataInfoMap))
                .collect(Collectors.toList());
    }

    /**
     * 获取推荐标签统计信息
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    public Map<String, Object> getRecommendationStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        long pendingCount = analysisRecommendTagRepository.countByUserIdAndUserAction(
                userId, AnalysisRecommendTag.UserAction.PENDING);
        long adoptedCount = analysisRecommendTagRepository.countByUserIdAndUserAction(
                userId, AnalysisRecommendTag.UserAction.ADOPTED);
        long ignoredCount = analysisRecommendTagRepository.countByUserIdAndUserAction(
                userId, AnalysisRecommendTag.UserAction.IGNORED);

        stats.put("pending", pendingCount);
        stats.put("adopted", adoptedCount);
        stats.put("ignored", ignoredCount);
        stats.put("total", pendingCount + adoptedCount + ignoredCount);

        return stats;
    }

    /**
     * 采纳推荐标签
     * 将推荐标签创建为新标签，并关联到技术领域
     *
     * @param recommendationId 推荐标签ID
     * @param userId           用户ID
     * @return 创建的新标签
     */
    @Transactional(rollbackFor = Exception.class)
    public Tag adoptRecommendation(Long recommendationId, Long userId) {
        AnalysisRecommendTag recommendTag = analysisRecommendTagRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("推荐标签不存在"));

        // 检查权限
        if (!recommendTag.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此推荐标签");
        }

        // 检查状态
        if (recommendTag.getUserAction() != AnalysisRecommendTag.UserAction.PENDING) {
            throw new IllegalArgumentException("该推荐标签已被处理");
        }

        // 检查标签名是否已存在
        TagDimension techDomain = tagDimensionRepository.findByCode("TECH_DOMAIN")
                .orElseThrow(() -> new IllegalStateException("技术领域维度不存在"));

        Optional<Tag> existingTag = tagRepository.findByNameAndDimensionId(
                recommendTag.getTagName(), techDomain.getId());

        if (existingTag.isPresent()) {
            // 如果标签已存在，只更新推荐状态，不创建新标签
            recommendTag.setUserAction(AnalysisRecommendTag.UserAction.ADOPTED);
            analysisRecommendTagRepository.save(recommendTag);
            log.info("推荐标签名称已存在，标记为已采纳: tagName={}", recommendTag.getTagName());
            return existingTag.get();
        }

        // 创建新标签
        Tag newTag = new Tag();
        newTag.setName(recommendTag.getTagName());
        newTag.setDimensionId(techDomain.getId());
        newTag.setLevel(1); // 默认作为一级标签（技术领域）
        newTag.setPath("/" + recommendTag.getTagName());
        newTag.setUserId(userId);
        newTag.setCreatedTime(LocalDateTime.now());
        newTag.setUpdatedTime(LocalDateTime.now());

        Tag savedTag = tagRepository.save(newTag);
        log.info("创建新标签成功: tagId={}, tagName={}", savedTag.getId(), savedTag.getName());

        // 更新推荐标签状态
        recommendTag.setUserAction(AnalysisRecommendTag.UserAction.ADOPTED);
        analysisRecommendTagRepository.save(recommendTag);

        // 将新标签关联到原分析结果
        AnalysisTag analysisTag = new AnalysisTag();
        analysisTag.setAnalysisResultId(recommendTag.getAnalysisResultId());
        analysisTag.setTagId(savedTag.getId());
        analysisTag.setIsPrimary(false);
        analysisTag.setConfidence(85);
        analysisTagRepository.save(analysisTag);

        return savedTag;
    }

    /**
     * 忽略推荐标签
     *
     * @param recommendationId 推荐标签ID
     * @param userId           用户ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean ignoreRecommendation(Long recommendationId, Long userId) {
        AnalysisRecommendTag recommendTag = analysisRecommendTagRepository.findById(recommendationId)
                .orElseThrow(() -> new IllegalArgumentException("推荐标签不存在"));

        // 检查权限
        if (!recommendTag.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此推荐标签");
        }

        // 只有待处理的可以忽略
        if (recommendTag.getUserAction() != AnalysisRecommendTag.UserAction.PENDING) {
            return false;
        }

        recommendTag.setUserAction(AnalysisRecommendTag.UserAction.IGNORED);
        analysisRecommendTagRepository.save(recommendTag);
        log.info("忽略推荐标签成功: recommendationId={}", recommendationId);

        return true;
    }

    /**
     * 批量采纳推荐标签
     *
     * @param ids    推荐标签ID列表
     * @param userId 用户ID
     * @return 创建的新标签列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Tag> adoptAllRecommendations(List<Long> ids, Long userId) {
        List<Tag> createdTags = new ArrayList<>();

        for (Long id : ids) {
            try {
                Tag tag = adoptRecommendation(id, userId);
                createdTags.add(tag);
            } catch (Exception e) {
                log.warn("批量采纳推荐标签失败: id={}, error={}", id, e.getMessage());
            }
        }

        log.info("批量采纳完成: 成功 {}/{} 个", createdTags.size(), ids.size());
        return createdTags;
    }

    /**
     * 批量忽略推荐标签
     *
     * @param ids    推荐标签ID列表
     * @param userId 用户ID
     * @return 成功忽略的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int ignoreAllRecommendations(List<Long> ids, Long userId) {
        int successCount = 0;

        for (Long id : ids) {
            try {
                if (ignoreRecommendation(id, userId)) {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("批量忽略推荐标签失败: id={}, error={}", id, e.getMessage());
            }
        }

        log.info("批量忽略完成: 成功 {}/{} 个", successCount, ids.size());
        return successCount;
    }

    /**
     * 转换为VO
     */
    private RecommendTagVO convertToVO(AnalysisRecommendTag recommendTag,
                                       Map<Long, AnalysisResult> analysisResultMap,
                                       Map<Long, DataInfo> dataInfoMap) {
        RecommendTagVO vo = new RecommendTagVO();
        vo.setId(recommendTag.getId());
        vo.setAnalysisResultId(recommendTag.getAnalysisResultId());
        vo.setTagName(recommendTag.getTagName());
        vo.setUserAction(recommendTag.getUserAction().name());
        vo.setUserActionText(RecommendTagVO.getActionText(recommendTag.getUserAction().name()));
        vo.setCreatedTime(recommendTag.getCreatedTime());
        vo.setUpdatedTime(recommendTag.getUpdatedTime());

        // 填充关联信息
        AnalysisResult analysisResult = analysisResultMap.get(recommendTag.getAnalysisResultId());
        if (analysisResult != null) {
            vo.setProblemStatement(analysisResult.getProblemStatement());

            DataInfo dataInfo = dataInfoMap.get(analysisResult.getFileId());
            if (dataInfo != null) {
                vo.setFileName(dataInfo.getFileName());
            }
        }

        return vo;
    }
}
