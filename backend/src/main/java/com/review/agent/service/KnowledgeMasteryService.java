package com.review.agent.service;

import com.review.agent.entity.pojo.KnowledgeMastery;
import com.review.agent.repository.KnowledgeMasteryRepository;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.vo.KnowledgeMasteryVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识点掌握度服务
 * 计算和追踪用户对各知识点的掌握程度
 */
@Slf4j
@Service
public class KnowledgeMasteryService {

    @Resource
    private KnowledgeMasteryRepository knowledgeMasteryRepository;

    @Resource
    private SecurityUtils  securityUtils;

    /**
     * 更新知识点掌握度
     * 基于答题历史计算加权掌握度（近期答题权重更高）
     *
     * @param knowledgePoint 知识点
     * @param isCorrect 是否正确
     * @param timeSpent 答题耗时（秒，可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMastery(String knowledgePoint, boolean isCorrect, Integer timeSpent) {
        if (knowledgePoint == null || knowledgePoint.trim().isEmpty()) {
            log.warn("知识点为空，跳过掌握度更新");
            return;
        }

        Long userId = securityUtils.getCurrentUserId();

        // 获取现有掌握度记录
        KnowledgeMastery existing = knowledgeMasteryRepository
                .findByUserIdAndKnowledgePoint(userId, knowledgePoint);

        // 计算新的掌握度
        BigDecimal newMasteryScore = calculateMasteryScore(existing, isCorrect, timeSpent);
        int totalAnswered = (existing != null ? existing.getTotalAnswered() : 0) + 1;
        int correctCount = (existing != null ? existing.getCorrectCount() : 0) + (isCorrect ? 1 : 0);
        Integer averageTime = calculateAverageTime(existing, timeSpent);

        // 插入或更新记录
        if (existing == null) {
            // 新增记录
            KnowledgeMastery mastery = new KnowledgeMastery();
            mastery.setUserId(userId);
            mastery.setKnowledgePoint(knowledgePoint);
            mastery.setTotalAnswered(totalAnswered);
            mastery.setCorrectCount(correctCount);
            mastery.setMasteryScore(newMasteryScore);
            mastery.setAverageTime(averageTime);
            mastery.setLastPracticeTime(LocalDateTime.now());
            knowledgeMasteryRepository.save(mastery);
            log.info("用户 {} 新增知识点 {}，掌握度: {}", userId, knowledgePoint, newMasteryScore);
        } else {
            // 更新现有记录
            existing.setTotalAnswered(totalAnswered);
            existing.setCorrectCount(correctCount);
            existing.setMasteryScore(newMasteryScore);
            existing.setAverageTime(averageTime);
            existing.setLastPracticeTime(LocalDateTime.now());
            knowledgeMasteryRepository.save(existing);
            log.info("用户 {} 更新知识点 {}，掌握度: {} -> {}", userId, knowledgePoint,
                    existing.getMasteryScore(), newMasteryScore);
        }
    }

    /**
     * 计算掌握度（加权算法）
     *
     * @param existing 现有掌握度记录
     * @param isCorrect 是否正确
     * @param timeSpent 答题耗时
     * @return 新的掌握度（0-100）
     */
    private BigDecimal calculateMasteryScore(KnowledgeMastery existing, boolean isCorrect, Integer timeSpent) {
        if (existing == null) {
            // 首次答题：正确得60分，错误得20分
            return isCorrect ? new BigDecimal("60.00") : new BigDecimal("20.00");
        }

        // 现有掌握度
        BigDecimal currentScore = existing.getMasteryScore();
        int totalAnswered = existing.getTotalAnswered();

        // 基础调整：正确加分，错误减分
        double adjustment = isCorrect ? 5.0 : -3.0;

        // 答题频次加权：答题越多，单次影响越小
        double frequencyWeight = 1.0 / Math.sqrt(totalAnswered + 1);

        // 时间因子：答题时间合理（在预期时间内）加分，过慢扣分
        double timeWeight = 1.0;
        if (timeSpent != null && existing.getAverageTime() != null) {
            int avgTime = existing.getAverageTime();
            if (timeSpent <= avgTime * 1.5) {
                timeWeight = 1.2; // 速度合理，加分
            } else if (timeSpent > avgTime * 2) {
                timeWeight = 0.8; // 速度过慢，扣分
            }
        }

        // 计算新分数
        double newScore = currentScore.doubleValue() + (adjustment * frequencyWeight * timeWeight);

        // 确保在 0-100 范围内
        newScore = Math.max(0, Math.min(100, newScore));

        // 连续答对奖励：如果正确且当前分数高于80，额外加分
        if (isCorrect && newScore > 80) {
            newScore += 2;
            newScore = Math.min(100, newScore);
        }

        return new BigDecimal(newScore).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算平均答题时间
     *
     * @param existing 现有记录
     * @param timeSpent 本次耗时
     * @return 新的平均时间
     */
    private Integer calculateAverageTime(KnowledgeMastery existing, Integer timeSpent) {
        if (timeSpent == null) {
            return existing != null ? existing.getAverageTime() : null;
        }

        if (existing == null || existing.getAverageTime() == null) {
            return timeSpent;
        }

        // 移动平均：最新数据权重更高
        int currentAvg = existing.getAverageTime();
        int totalAnswered = existing.getTotalAnswered();

        // 新平均 = (旧平均 * (n-1) + 新数据) / n
        return (currentAvg * totalAnswered + timeSpent) / (totalAnswered + 1);
    }

    /**
     * 获取用户所有知识点掌握度记录
     *
     * @return 掌握度列表
     */
    public List<KnowledgeMastery> getUserKnowledgeMap() {
        Long userId = securityUtils.getCurrentUserId();
        return knowledgeMasteryRepository.findByUserId(userId);
    }

    /**
     * 获取用户薄弱知识点（掌握度最低的N个）
     *
     * @param limit 返回数量
     * @return 薄弱知识点列表
     */
    public List<KnowledgeMastery> getWeakKnowledgePoints(int limit) {
        Long userId = securityUtils.getCurrentUserId();
        List<KnowledgeMastery> weakPoints = knowledgeMasteryRepository
                .findWeakestByUserIdLimit(userId, limit);

        // 过滤掉答题次数过少的（少于3次的不准确）
        return weakPoints.stream()
                .filter(km -> km.getTotalAnswered() >= 3)
                .collect(Collectors.toList());
    }

    /**
     * 获取掌握度低于阈值的知识点
     *
     * @param threshold 阈值（如 60.00）
     * @return 掌握度低于阈值的知识点
     */
    public List<KnowledgeMastery> getBelowThreshold(BigDecimal threshold) {
        Long userId = securityUtils.getCurrentUserId();
        return knowledgeMasteryRepository.findBelowThresholdByUserId(userId, threshold);
    }

    /**
     * 获取知识点掌握度统计
     *
     * @return 统计Map
     */
    public Map<String, Object> getMasteryStats() {
        Long userId = securityUtils.getCurrentUserId();
        List<KnowledgeMastery> masteries = knowledgeMasteryRepository.findByUserId(userId);

        if (masteries.isEmpty()) {
            return Map.of(
                    "totalKnowledgePoints", 0,
                    "averageMastery", 0,
                    "weakPointsCount", 0,
                    "strongPointsCount", 0
            );
        }

        // 计算平均掌握度
        double avgMastery = masteries.stream()
                .mapToDouble(km -> km.getMasteryScore().doubleValue())
                .average()
                .orElse(0);

        // 统计薄弱和强势知识点
        long weakCount = masteries.stream()
                .filter(km -> km.getMasteryScore().compareTo(new BigDecimal("60")) < 0)
                .count();

        long strongCount = masteries.stream()
                .filter(km -> km.getMasteryScore().compareTo(new BigDecimal("80")) >= 0)
                .count();

        return Map.of(
                "totalKnowledgePoints", masteries.size(),
                "averageMastery", BigDecimal.valueOf(avgMastery).setScale(2, RoundingMode.HALF_UP),
                "weakPointsCount", weakCount,
                "strongPointsCount", strongCount
        );
    }

    /**
     * 获取用户最近练习的知识点
     *
     * @return 最近练习的知识点列表
     */
    public List<KnowledgeMastery> getRecentlyPracticed(int limit) {
        Long userId = securityUtils.getCurrentUserId();
        List<KnowledgeMastery> practiced = knowledgeMasteryRepository.findRecentlyPracticedByUserId(userId);

        return practiced.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    // ========== 新增方法：返回VO格式 ==========

    /**
     * 获取用户的知识点掌握度列表（VO格式）
     *
     * @param userId 用户ID
     * @param limit  限制数量
     * @return 掌握度列表
     */
    public List<KnowledgeMasteryVO> getUserKnowledgeMastery(Long userId, int limit) {
        List<KnowledgeMastery> masteries = knowledgeMasteryRepository.findByUserId(userId);

        // 转换为VO
        List<KnowledgeMasteryVO> masteryList = masteries.stream()
            .map(km -> KnowledgeMasteryVO.builder()
                .knowledgePoint(km.getKnowledgePoint())
                .masteryRate(km.getMasteryScore().doubleValue())
                .correctCount(km.getCorrectCount())
                .totalCount(km.getTotalAnswered())
                .build())
            .sorted(Comparator.comparing(KnowledgeMasteryVO::getMasteryRate))
            .limit(limit)
            .collect(Collectors.toList());

        return masteryList;
    }

    /**
     * 获取用户的薄弱知识点（VO格式）
     *
     * @param userId 用户ID
     * @param limit  限制数量
     * @return 薄弱知识点列表
     */
    public List<KnowledgeMasteryVO> getWeakKnowledgePoints(Long userId, int limit) {
        List<KnowledgeMastery> weakPoints = knowledgeMasteryRepository.findWeakestByUserIdLimit(userId, 100);

        // 过滤掉答题次数过少的（少于3次的不准确）
        return weakPoints.stream()
            .filter(km -> km.getTotalAnswered() >= 3)
            .map(km -> KnowledgeMasteryVO.builder()
                .knowledgePoint(km.getKnowledgePoint())
                .masteryRate(km.getMasteryScore().doubleValue())
                .correctCount(km.getCorrectCount())
                .totalCount(km.getTotalAnswered())
                .build())
            .limit(limit)
            .toList();
    }
}
