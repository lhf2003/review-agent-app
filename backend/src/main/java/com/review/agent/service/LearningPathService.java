package com.review.agent.service;

import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.*;
import com.review.agent.entity.vo.LearningPathVO;
import com.review.agent.entity.vo.LearningPathVO.LearningStepVO;
import com.review.agent.entity.vo.LearningPathVO.PathNodeVO;
import com.review.agent.repository.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习路径服务
 * 基于标签关系构建个性化学习路径
 */
@Slf4j
@Service
public class LearningPathService {

    @Resource
    private TagRelationRepository tagRelationRepository;

    @Resource
    private TagRepository tagRepository;

    @Resource
    private QuizQuestionRepository quizQuestionRepository;

    @Resource
    private MistakeRepository mistakeRepository;

    @Resource
    private KnowledgeMasteryRepository knowledgeMasteryRepository;

    @Resource
    private SecurityUtils securityUtils;

    /**
     * 获取用户的学习路径推荐
     * 基于用户的薄弱标签和标签关系构建
     */
    public List<LearningPathVO> getLearningPathRecommendations(int limit) {
        Long userId = securityUtils.getCurrentUserId();

        // 1. 获取用户的薄弱标签（掌握度 < 60 的标签）
        List<KnowledgeMastery> weakMasteries = knowledgeMasteryRepository
                .findBelowThresholdByUserId(userId, BigDecimal.valueOf(60));

        if (weakMasteries.isEmpty()) {
            // 如果没有薄弱标签，推荐进阶学习
            return getAdvancedLearningPaths(userId, limit);
        }

        // 2. 为每个薄弱标签构建学习路径
        List<LearningPathVO> paths = new ArrayList<>();
        Set<Long> processedTagIds = new HashSet<>();

        for (KnowledgeMastery mastery : weakMasteries) {
            if (paths.size() >= limit) break;
            if (mastery.getTagId() == null) continue;
            if (processedTagIds.contains(mastery.getTagId())) continue;

            LearningPathVO path = buildLearningPathForTag(mastery.getTagId(), userId);
            if (path != null) {
                paths.add(path);
                processedTagIds.add(mastery.getTagId());
            }
        }

        // 3. 按完成度排序（优先推荐完成度低的，即需要优先学习的）
        paths.sort(Comparator.comparingInt(LearningPathVO::getCompletionRate));

        return paths;
    }

    /**
     * 为特定标签构建学习路径
     */
    public LearningPathVO buildLearningPathForTag(Long targetTagId, Long userId) {
        Tag targetTag = tagRepository.findById(targetTagId).orElse(null);
        if (targetTag == null) return null;

        // 获取该标签的掌握度
        KnowledgeMastery mastery = knowledgeMasteryRepository
                .findByUserIdAndTagId(userId, targetTagId)
                .orElse(null);

        int masteryLevel = mastery != null ? mastery.getMasteryScore().intValue() : 0;

        // 构建路径
        LearningPathVO.LearningPathVOBuilder pathBuilder = LearningPathVO.builder()
                .id(targetTagId)
                .title(targetTag.getName())
                .description(generatePathDescription(targetTag))
                .difficulty(calculateDifficulty(masteryLevel))
                .completionRate(masteryLevel)
                .createdAt(java.time.LocalDateTime.now());

        // 1. 获取前置依赖
        List<TagRelation> dependencies = tagRelationRepository
                .findDependenciesByTargetTagId(targetTagId);

        List<PathNodeVO> prerequisites = dependencies.stream()
                .map(r -> buildPathNode(r.getSourceTagId(), userId, r.getStrength(), "DEPENDS_ON"))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(PathNodeVO::getRelationStrength).reversed())
                .limit(5)
                .collect(Collectors.toList());

        pathBuilder.prerequisites(prerequisites);

        // 2. 构建学习步骤
        List<LearningStepVO> steps = buildLearningSteps(targetTagId, dependencies, userId);
        pathBuilder.steps(steps);

        // 3. 获取推荐并行学习的标签（相似标签）
        List<PathNodeVO> parallelTags = findParallelTags(targetTagId, userId);
        pathBuilder.parallelTags(parallelTags);

        // 4. 获取进阶方向
        List<PathNodeVO> nextSteps = findNextSteps(targetTagId, userId);
        pathBuilder.nextSteps(nextSteps);

        // 5. 计算题目数量
        int questionCount = countQuestionsForTag(targetTagId);
        pathBuilder.questionCount(questionCount);

        // 6. 生成推荐原因
        String reason = generateRecommendationReason(targetTag, masteryLevel, prerequisites);
        pathBuilder.reason(reason);

        // 7. 计算预计学习时间
        int estimatedMinutes = calculateEstimatedTime(steps.size(), questionCount);
        pathBuilder.estimatedMinutes(estimatedMinutes);

        // 8. 确定路径类型
        String pathType = determinePathType(prerequisites.size(), parallelTags.size());
        pathBuilder.pathType(pathType);

        return pathBuilder.build();
    }

    /**
     * 构建学习步骤
     */
    private List<LearningStepVO> buildLearningSteps(Long targetTagId,
                                                     List<TagRelation> dependencies,
                                                     Long userId) {
        List<LearningStepVO> steps = new ArrayList<>();
        int order = 1;

        // 获取目标标签名称（用于前置步骤描述）
        Tag targetTagForDesc = tagRepository.findById(targetTagId).orElse(null);
        String targetTagName = targetTagForDesc != null ? targetTagForDesc.getName() : "目标";

        // 1. 添加前置依赖作为前置步骤
        for (TagRelation dep : dependencies.stream()
                .sorted(Comparator.comparingInt(TagRelation::getStrength).reversed())
                .limit(3)
                .toList()) {

            Tag depTag = tagRepository.findById(dep.getSourceTagId()).orElse(null);
            if (depTag == null) continue;

            KnowledgeMastery depMastery = knowledgeMasteryRepository
                    .findByUserIdAndTagId(userId, depTag.getId())
                    .orElse(null);

            int depMasteryLevel = depMastery != null ? depMastery.getMasteryScore().intValue() : 0;

            steps.add(LearningStepVO.builder()
                    .order(order++)
                    .tagId(depTag.getId())
                    .tagName(depTag.getName())
                    .stepType("prerequisite")
                    .completed(depMasteryLevel >= 60)
                    .masteryLevel(depMasteryLevel)
                    .questionCount(countQuestionsForTag(depTag.getId()))
                    .description("前置知识点：学习「" + targetTagName + "」需要先掌握")
                    .build());
        }

        // 2. 添加核心目标
        Tag targetTag = tagRepository.findById(targetTagId).orElse(null);
        KnowledgeMastery targetMastery = knowledgeMasteryRepository
                .findByUserIdAndTagId(userId, targetTagId)
                .orElse(null);

        int targetMasteryLevel = targetMastery != null ? targetMastery.getMasteryScore().intValue() : 0;

        steps.add(LearningStepVO.builder()
                .order(order++)
                .tagId(targetTagId)
                .tagName(targetTag != null ? targetTag.getName() : "目标")
                .stepType("core")
                .completed(targetMasteryLevel >= 80)
                .masteryLevel(targetMasteryLevel)
                .questionCount(countQuestionsForTag(targetTagId))
                .description("核心学习目标")
                .build());

        return steps;
    }

    /**
     * 查找并行学习的标签（相似标签）
     */
    private List<PathNodeVO> findParallelTags(Long tagId, Long userId) {
        // 获取相似标签
        List<TagRelation> similarRelations = tagRelationRepository.findBySourceTagId(tagId).stream()
                .filter(r -> r.getRelationType() == TagRelation.RelationType.SIMILAR_TO)
                .limit(3)
                .toList();

        List<TagRelation> targetSimilar = tagRelationRepository.findByTargetTagId(tagId).stream()
                .filter(r -> r.getRelationType() == TagRelation.RelationType.SIMILAR_TO)
                .limit(3)
                .toList();

        List<TagRelation> allSimilar = new ArrayList<>(similarRelations);
        allSimilar.addAll(targetSimilar);

        return allSimilar.stream()
                .map(r -> {
                    Long relatedTagId = r.getSourceTagId().equals(tagId)
                            ? r.getTargetTagId() : r.getSourceTagId();
                    return buildPathNode(relatedTagId, userId, r.getStrength(), "SIMILAR_TO");
                })
                .filter(Objects::nonNull)
                .distinct()
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * 查找进阶方向
     */
    private List<PathNodeVO> findNextSteps(Long tagId, Long userId) {
        // 获取被当前标签依赖的标签（即学完当前标签后可以学的）
        List<TagRelation> dependents = tagRelationRepository
                .findDependentsBySourceTagId(tagId);

        return dependents.stream()
                .map(r -> buildPathNode(r.getTargetTagId(), userId, r.getStrength(), "DEPENDS_ON"))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(PathNodeVO::getRelationStrength).reversed())
                .limit(3)
                .collect(Collectors.toList());
    }

    /**
     * 构建路径节点
     */
    private PathNodeVO buildPathNode(Long tagId, Long userId, Integer strength, String relationType) {
        Tag tag = tagRepository.findById(tagId).orElse(null);
        if (tag == null) return null;

        KnowledgeMastery mastery = knowledgeMasteryRepository
                .findByUserIdAndTagId(userId, tagId)
                .orElse(null);

        int nodeMasteryLevel = mastery != null && mastery.getMasteryScore() != null
                ? mastery.getMasteryScore().intValue() : 0;

        return PathNodeVO.builder()
                .tagId(tagId)
                .tagName(tag.getName())
                .masteryLevel(nodeMasteryLevel)
                .relationStrength(strength)
                .relationType(relationType)
                .build();
    }

    /**
     * 获取进阶学习路径（当用户没有薄弱标签时）
     */
    private List<LearningPathVO> getAdvancedLearningPaths(Long userId, int limit) {
        // 获取用户已掌握的高阶标签（掌握度 >= 80）
        List<KnowledgeMastery> masteredTags = knowledgeMasteryRepository
                .findByUserIdAndMasteryScoreGreaterThanEqual(userId, BigDecimal.valueOf(80));

        if (masteredTags.isEmpty()) {
            return Collections.emptyList();
        }

        List<LearningPathVO> paths = new ArrayList<>();
        Set<Long> processed = new HashSet<>();

        for (KnowledgeMastery mastery : masteredTags) {
            if (paths.size() >= limit) break;

            // 查找进阶方向
            List<TagRelation> nextSteps = tagRelationRepository
                    .findDependentsBySourceTagId(mastery.getTagId());

            for (TagRelation relation : nextSteps) {
                if (processed.contains(relation.getTargetTagId())) continue;

                // 检查是否已掌握
                KnowledgeMastery nextMastery = knowledgeMasteryRepository
                        .findByUserIdAndTagId(userId, relation.getTargetTagId())
                        .orElse(null);

                int nextMasteryLevel = nextMastery != null && nextMastery.getMasteryScore() != null
                        ? nextMastery.getMasteryScore().intValue() : 0;

                if (nextMastery == null || nextMasteryLevel < 60) {
                    LearningPathVO path = buildLearningPathForTag(relation.getTargetTagId(), userId);
                    if (path != null) {
                        path.setReason("进阶学习：基于你已掌握的「" +
                                tagRepository.findById(mastery.getTagId())
                                        .map(Tag::getName).orElse("") + "」推荐");
                        paths.add(path);
                        processed.add(relation.getTargetTagId());
                    }
                }

                if (paths.size() >= limit) break;
            }
        }

        return paths;
    }

    /**
     * 计算标签相关题目数量
     */
    private int countQuestionsForTag(Long tagId) {
        return (int) quizQuestionRepository.countByTagId(tagId);
    }

    /**
     * 计算难度等级
     */
    private String calculateDifficulty(int masteryLevel) {
        if (masteryLevel >= 80) return "expert";
        if (masteryLevel >= 60) return "advanced";
        if (masteryLevel >= 40) return "intermediate";
        return "beginner";
    }

    /**
     * 计算预计学习时间
     */
    private int calculateEstimatedTime(int stepCount, int questionCount) {
        // 每个步骤30分钟，每道题5分钟
        return stepCount * 30 + questionCount * 5;
    }

    /**
     * 确定路径类型
     */
    private String determinePathType(int prerequisiteCount, int parallelCount) {
        if (prerequisiteCount == 0) return "beginner";
        if (parallelCount > 0) return "parallel";
        return "sequential";
    }

    /**
     * 生成路径描述
     */
    private String generatePathDescription(Tag tag) {
        if (tag.getDescription() != null && !tag.getDescription().isEmpty()) {
            return tag.getDescription();
        }
        return "学习「" + tag.getName() + "」的完整路径";
    }

    /**
     * 生成推荐原因
     */
    private String generateRecommendationReason(Tag tag, int masteryLevel, List<PathNodeVO> prerequisites) {
        StringBuilder reason = new StringBuilder();

        if (masteryLevel < 40) {
            reason.append("薄弱知识点：掌握度仅").append(masteryLevel).append("%，建议优先学习");
        } else if (masteryLevel < 60) {
            reason.append("待提升：掌握度").append(masteryLevel).append("%，需要加强练习");
        } else {
            reason.append("进阶提升：巩固「").append(tag.getName()).append("」知识");
        }

        if (!prerequisites.isEmpty()) {
            long uncompletedPrereqs = prerequisites.stream()
                    .filter(p -> p.getMasteryLevel() < 60)
                    .count();
            if (uncompletedPrereqs > 0) {
                reason.append("，需要先完成 ").append(uncompletedPrereqs).append(" 个前置知识点");
            }
        }

        return reason.toString();
    }

    /**
     * 获取特定标签的学习路径
     */
    public LearningPathVO getLearningPathForTag(Long tagId) {
        Long userId = securityUtils.getCurrentUserId();
        return buildLearningPathForTag(tagId, userId);
    }

    /**
     * 获取用户当前的学习进度概览
     */
    public Map<String, Object> getLearningProgressOverview() {
        Long userId = securityUtils.getCurrentUserId();

        // 统计信息
        long totalMasteries = knowledgeMasteryRepository.countByUserId(userId);
        long masteredCount = knowledgeMasteryRepository
                .countByUserIdAndMasteryScoreGreaterThanEqual(userId, BigDecimal.valueOf(80));
        long learningCount = knowledgeMasteryRepository
                .countByUserIdAndMasteryScoreBetween(userId, BigDecimal.valueOf(40), BigDecimal.valueOf(79));
        long weakCount = knowledgeMasteryRepository
                .countByUserIdAndMasteryScoreLessThan(userId, BigDecimal.valueOf(40));

        // 计算总体完成度
        int overallProgress = totalMasteries > 0
                ? (int) ((masteredCount * 100 + learningCount * 60) / totalMasteries)
                : 0;

        // 正在学习的路径
        List<LearningPathVO> activePaths = getLearningPathRecommendations(3);

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalTags", totalMasteries);
        overview.put("masteredCount", masteredCount);
        overview.put("learningCount", learningCount);
        overview.put("weakCount", weakCount);
        overview.put("overallProgress", Math.min(100, overallProgress));
        overview.put("activePaths", activePaths);

        return overview;
    }
}
