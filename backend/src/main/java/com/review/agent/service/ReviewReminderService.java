package com.review.agent.service;

import com.review.agent.entity.pojo.Mistake;
import com.review.agent.entity.pojo.NotificationSettings;
import com.review.agent.entity.pojo.QuizQuestion;
import com.review.agent.entity.pojo.QuizRecord;
import com.review.agent.entity.vo.NotificationSettingsVO;
import com.review.agent.entity.vo.PendingReviewVO;
import com.review.agent.repository.MistakeRepository;
import com.review.agent.repository.NotificationSettingsRepository;
import com.review.agent.repository.QuizQuestionRepository;
import com.review.agent.repository.QuizRecordRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 复习提醒服务
 * 基于艾宾浩斯遗忘曲线计算复习时间
 */
@Slf4j
@Service
public class ReviewReminderService {

    /**
     * 艾宾浩斯遗忘曲线复习间隔（天）
     * 第1次复习：1天后
     * 第2次复习：3天后
     * 第3次复习：7天后
     * 第4次复习：15天后
     * 第5次复习：30天后
     */
    private static final int[] EBBINGHAUS_INTERVALS = {1, 3, 7, 15, 30};

    @Resource
    private NotificationSettingsRepository notificationSettingsRepository;

    @Resource
    private MistakeRepository mistakeRepository;

    @Resource
    private QuizRecordRepository quizRecordRepository;

    @Resource
    private QuizQuestionRepository quizQuestionRepository;

    /**
     * 获取用户的通知设置
     *
     * @param userId 用户ID
     * @return 通知设置VO
     */
    public NotificationSettingsVO getNotificationSettings(Long userId) {
        Optional<NotificationSettings> settings = notificationSettingsRepository.findByUserId(userId);

        if (settings.isEmpty()) {
            // 返回默认设置
            return NotificationSettingsVO.builder()
                    .browserNotificationEnabled(false)
                    .reminderFrequencyDays(1)
                    .reminderHour(9)
                    .mistakeReviewEnabled(true)
                    .quizCompletionEnabled(true)
                    .updatedTime(LocalDateTime.now())
                    .build();
        }

        NotificationSettings ns = settings.get();
        return NotificationSettingsVO.builder()
                .browserNotificationEnabled(ns.getBrowserNotificationEnabled())
                .reminderFrequencyDays(ns.getReminderFrequencyDays())
                .reminderHour(ns.getReminderHour())
                .mistakeReviewEnabled(ns.getMistakeReviewEnabled())
                .quizCompletionEnabled(ns.getQuizCompletionEnabled())
                .updatedTime(ns.getUpdatedTime())
                .build();
    }

    /**
     * 更新用户的通知设置
     *
     * @param userId 用户ID
     * @param vo     通知设置VO
     * @return 更新后的设置
     */
    @Transactional(rollbackFor = Exception.class)
    public NotificationSettingsVO updateNotificationSettings(Long userId, NotificationSettingsVO vo) {
        NotificationSettings settings = notificationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    NotificationSettings newSettings = new NotificationSettings();
                    newSettings.setUserId(userId);
                    return newSettings;
                });

        settings.setBrowserNotificationEnabled(vo.getBrowserNotificationEnabled());
        settings.setReminderFrequencyDays(vo.getReminderFrequencyDays());
        settings.setReminderHour(vo.getReminderHour());
        settings.setMistakeReviewEnabled(vo.getMistakeReviewEnabled());
        settings.setQuizCompletionEnabled(vo.getQuizCompletionEnabled());

        settings = notificationSettingsRepository.save(settings);

        return NotificationSettingsVO.builder()
                .browserNotificationEnabled(settings.getBrowserNotificationEnabled())
                .reminderFrequencyDays(settings.getReminderFrequencyDays())
                .reminderHour(settings.getReminderHour())
                .mistakeReviewEnabled(settings.getMistakeReviewEnabled())
                .quizCompletionEnabled(settings.getQuizCompletionEnabled())
                .updatedTime(settings.getUpdatedTime())
                .build();
    }

    /**
     * 获取待复习列表
     * 基于遗忘曲线计算需要复习的错题和习题
     *
     * @param userId 用户ID
     * @return 待复习项列表
     */
    public List<PendingReviewVO> getPendingReviews(Long userId) {
        List<PendingReviewVO> pendingReviews = new ArrayList<>();

        // 获取用户的通知设置
        NotificationSettingsVO settings = getNotificationSettings(userId);

        // 获取待复习的错题
        if (Boolean.TRUE.equals(settings.getMistakeReviewEnabled())) {
            List<PendingReviewVO> mistakeReviews = getPendingMistakeReviews(userId);
            pendingReviews.addAll(mistakeReviews);
        }

        // 获取待复习的习题（基于已完成但分数较低的测验）
        if (Boolean.TRUE.equals(settings.getQuizCompletionEnabled())) {
            List<PendingReviewVO> quizReviews = getPendingQuizReviews(userId);
            pendingReviews.addAll(quizReviews);
        }

        // 按优先级和下次复习时间排序
        pendingReviews.sort(Comparator
                .comparing(PendingReviewVO::getPriority).reversed()
                .thenComparing(PendingReviewVO::getNextReviewTime));

        return pendingReviews;
    }

    /**
     * 获取待复习的错题
     *
     * @param userId 用户ID
     * @return 错题复习列表
     */
    private List<PendingReviewVO> getPendingMistakeReviews(Long userId) {
        List<Mistake> unmasteredMistakes = mistakeRepository.findUnmasteredByUserId(userId);
        List<PendingReviewVO> result = new ArrayList<>();

        for (Mistake mistake : unmasteredMistakes) {
            // 计算复习阶段和下次复习时间
            int reviewStage = calculateReviewStage(mistake.getCreatedTime(), mistake.getLastMistakeTime());
            LocalDateTime nextReviewTime = calculateNextReviewTime(
                    mistake.getLastMistakeTime() != null ? mistake.getLastMistakeTime() : mistake.getCreatedTime(),
                    reviewStage
            );

            // 只返回需要复习的项目（下次复习时间已过或即将到来）
            LocalDateTime now = LocalDateTime.now();
            if (nextReviewTime.isBefore(now.plusHours(24))) {
                // 获取题目详情
                QuizQuestion question = quizQuestionRepository.findById(mistake.getQuestionId()).orElse(null);
                if (question != null) {
                    PendingReviewVO vo = PendingReviewVO.builder()
                            .type("MISTAKE")
                            .referenceId(mistake.getId())
                            .title(truncateTitle(question.getQuestionText(), 50))
                            .knowledgePoint(question.getKnowledgePoint())
                            .priority(calculatePriority(mistake.getMistakeCount(), reviewStage))
                            .lastReviewTime(mistake.getLastMistakeTime())
                            .nextReviewTime(nextReviewTime)
                            .reviewStage(reviewStage + 1)
                            .mistakeCount(mistake.getMistakeCount())
                            .build();
                    result.add(vo);
                }
            }
        }

        return result;
    }

    /**
     * 获取待复习的习题
     * 基于已完成的测验，分数低于80分的建议复习
     *
     * @param userId 用户ID
     * @return 习题复习列表
     */
    private List<PendingReviewVO> getPendingQuizReviews(Long userId) {
        // 获取用户所有已完成的测验
        List<QuizRecord> completedQuizzes = quizRecordRepository.findAllByUserIdAndStatusOrderByCreatedTimeAsc(userId, 1);
        List<PendingReviewVO> result = new ArrayList<>();

        for (QuizRecord quiz : completedQuizzes) {
            // 只建议复习分数低于80分的测验
            if (quiz.getTotalScore() != null && quiz.getTotalScore() < 80) {
                // 计算复习阶段
                int reviewStage = calculateReviewStage(quiz.getCreatedTime(), quiz.getUpdatedTime());
                LocalDateTime nextReviewTime = calculateNextReviewTime(
                        quiz.getUpdatedTime() != null ? quiz.getUpdatedTime() : quiz.getCreatedTime(),
                        reviewStage
                );

                LocalDateTime now = LocalDateTime.now();
                if (nextReviewTime.isBefore(now.plusHours(24))) {
                    PendingReviewVO vo = PendingReviewVO.builder()
                            .type("QUIZ")
                            .referenceId(quiz.getId())
                            .title("测验复习（分数：" + quiz.getTotalScore() + "）")
                            .knowledgePoint(null) // 可以从题目中提取
                            .priority(calculateQuizPriority(quiz.getTotalScore(), reviewStage))
                            .lastReviewTime(quiz.getUpdatedTime())
                            .nextReviewTime(nextReviewTime)
                            .reviewStage(reviewStage + 1)
                            .quizScore(quiz.getTotalScore())
                            .build();
                    result.add(vo);
                }
            }
        }

        return result;
    }

    /**
     * 计算当前复习阶段
     *
     * @param createdTime      创建时间
     * @param lastReviewTime   上次复习时间
     * @return 当前阶段（0-4）
     */
    private int calculateReviewStage(LocalDateTime createdTime, LocalDateTime lastReviewTime) {
        if (createdTime == null) return 0;

        LocalDateTime baseTime = lastReviewTime != null ? lastReviewTime : createdTime;
        LocalDateTime now = LocalDateTime.now();
        long daysPassed = java.time.Duration.between(baseTime, now).toDays();

        // 根据已过天数判断当前阶段
        int stage = 0;
        int accumulatedDays = 0;
        for (int i = 0; i < EBBINGHAUS_INTERVALS.length; i++) {
            accumulatedDays += EBBINGHAUS_INTERVALS[i];
            if (daysPassed >= accumulatedDays) {
                stage = i + 1;
            } else {
                break;
            }
        }

        return Math.min(stage, EBBINGHAUS_INTERVALS.length - 1);
    }

    /**
     * 计算下次复习时间
     *
     * @param lastReviewTime 上次复习时间
     * @param currentStage   当前阶段
     * @return 下次复习时间
     */
    private LocalDateTime calculateNextReviewTime(LocalDateTime lastReviewTime, int currentStage) {
        if (lastReviewTime == null) {
            lastReviewTime = LocalDateTime.now();
        }

        int intervalDays = EBBINGHAUS_INTERVALS[Math.min(currentStage, EBBINGHAUS_INTERVALS.length - 1)];
        return lastReviewTime.plusDays(intervalDays);
    }

    /**
     * 计算复习优先级
     *
     * @param mistakeCount 错误次数
     * @param reviewStage  复习阶段
     * @return 优先级（1-5）
     */
    private int calculatePriority(Integer mistakeCount, int reviewStage) {
        int count = mistakeCount != null ? mistakeCount : 1;
        // 错误次数越多，阶段越早，优先级越高
        int basePriority = Math.min(count, 3) + (EBBINGHAUS_INTERVALS.length - reviewStage);
        return Math.min(Math.max(basePriority, 1), 5);
    }

    /**
     * 计算测验复习优先级
     *
     * @param score       测验分数
     * @param reviewStage 复习阶段
     * @return 优先级（1-5）
     */
    private int calculateQuizPriority(Integer score, int reviewStage) {
        if (score == null) return 3;
        // 分数越低，优先级越高
        int scoreFactor = (100 - score) / 25;
        int stageFactor = EBBINGHAUS_INTERVALS.length - reviewStage;
        return Math.min(Math.max(scoreFactor + stageFactor, 1), 5);
    }

    /**
     * 截断标题
     *
     * @param text    原文本
     * @param maxSize 最大长度
     * @return 截断后的文本
     */
    private String truncateTitle(String text, int maxSize) {
        if (text == null) return "";
        if (text.length() <= maxSize) return text;
        return text.substring(0, maxSize) + "...";
    }

    /**
     * 获取待复习数量统计
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    public PendingReviewStats getPendingReviewStats(Long userId) {
        List<PendingReviewVO> pendingReviews = getPendingReviews(userId);

        int totalPending = pendingReviews.size();
        int mistakeCount = (int) pendingReviews.stream()
                .filter(r -> "MISTAKE".equals(r.getType()))
                .count();
        int quizCount = (int) pendingReviews.stream()
                .filter(r -> "QUIZ".equals(r.getType()))
                .count();
        int highPriorityCount = (int) pendingReviews.stream()
                .filter(r -> r.getPriority() != null && r.getPriority() >= 4)
                .count();

        return new PendingReviewStats(totalPending, mistakeCount, quizCount, highPriorityCount);
    }

    /**
     * 待复习统计信息
     */
    public record PendingReviewStats(
            int totalPending,
            int mistakeCount,
            int quizCount,
            int highPriorityCount
    ) {}
}
