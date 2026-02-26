package com.review.agent.service;

import com.review.agent.entity.pojo.Mistake;
import com.review.agent.entity.pojo.MistakeHistory;
import com.review.agent.entity.pojo.QuizQuestion;
import com.review.agent.entity.vo.MistakeHistoryVO;
import com.review.agent.entity.vo.MistakeVo;
import com.review.agent.entity.vo.ReviewRecommendationVO;
import com.review.agent.repository.MistakeHistoryRepository;
import com.review.agent.repository.MistakeRepository;
import com.review.agent.repository.QuizQuestionRepository;
import com.review.agent.common.utils.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 错题本服务
 * 实现错题记录、复习、遗忘曲线推荐等功能
 */
@Slf4j
@Service
public class MistakeBookService {

    @Resource
    private MistakeRepository mistakeRepository;

    @Resource
    private QuizQuestionRepository quizQuestionRepository;

    @Resource
    private MistakeHistoryRepository mistakeHistoryRepository;

    @Resource
    private SecurityUtils securityUtils;

    /**
     * 记录错题
     * 如果答错，自动加入错题本；如果答对，更新掌握状态
     *
     * @param questionId 题目ID
     * @param isCorrect 是否正确
     * @param quizId 来源测验ID（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordAnswer(Long questionId, boolean isCorrect, Long quizId) {
        Long userId = securityUtils.getCurrentUserId();

        List<Mistake> existingMistakes = mistakeRepository.findByUserIdAndQuestionId(userId, questionId);

        if (isCorrect) {
            // 答对了：检查是否连续答对3次，标记为掌握
            if (!existingMistakes.isEmpty()) {
                Mistake mistake = existingMistakes.get(0);
                // TODO 这里需要额外的字段来跟踪连续正确次数，暂时简化处理
                // 实际应该记录连续正确次数，达到3次才标记为掌握
                log.info("用户 {} 答对了题目 {}，考虑标记为掌握", userId, questionId);
            }
        } else {
            // 答错了：更新错题记录
            if (existingMistakes.isEmpty()) {
                // 新增错题记录
                Mistake mistake = new Mistake();
                mistake.setUserId(userId);
                mistake.setQuestionId(questionId);
                mistake.setQuizId(quizId);
                mistake.setMistakeCount(1);
                mistake.setLastMistakeTime(LocalDateTime.now());
                mistake.setMastered(false);
                mistakeRepository.save(mistake);
                log.info("用户 {} 新增错题 {}", userId, questionId);
            } else {
                // 更新已有错题记录
                Mistake mistake = existingMistakes.get(0);
                mistake.setMistakeCount(mistake.getMistakeCount() + 1);
                mistake.setLastMistakeTime(LocalDateTime.now());
                mistake.setMastered(false);
                mistakeRepository.save(mistake);
                log.info("用户 {} 错题 {} 次数更新为 {}", userId, questionId, mistake.getMistakeCount());
            }
        }
    }

    /**
     * 记录答题历史（包含详细的答题信息）
     *
     * @param questionId 题目ID
     * @param isCorrect 是否正确
     * @param wrongAnswer 错误答案（可选）
     * @param correctAnswer 正确答案（可选）
     * @param timeSpent 答题用时（秒，可选）
     * @param quizId 来源测验ID（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordAnswerHistory(Long questionId, boolean isCorrect,
                                     String wrongAnswer, String correctAnswer,
                                     Integer timeSpent, Long quizId) {
        Long userId = securityUtils.getCurrentUserId();

        // 先调用原有的错题记录逻辑
        recordAnswer(questionId, isCorrect, quizId);

        // 获取或创建错题记录ID
        List<Mistake> mistakes = mistakeRepository.findByUserIdAndQuestionId(userId, questionId);
        Long mistakeId = null;

        if (mistakes.isEmpty()) {
            // 如果没有错题记录且答错了，创建一个新的
            if (!isCorrect) {
                Mistake newMistake = new Mistake();
                newMistake.setUserId(userId);
                newMistake.setQuestionId(questionId);
                newMistake.setQuizId(quizId);
                newMistake.setMistakeCount(1);
                newMistake.setLastMistakeTime(LocalDateTime.now());
                newMistake.setMastered(false);
                newMistake = mistakeRepository.save(newMistake);
                mistakeId = newMistake.getId();
            }
        } else {
            mistakeId = mistakes.get(0).getId();
        }

        // 只有存在错题记录时才记录历史
        if (mistakeId != null) {
            MistakeHistory history = new MistakeHistory();
            history.setUserId(userId);
            history.setMistakeId(mistakeId);
            history.setQuestionId(questionId);
            history.setQuizId(quizId);
            history.setWrongAnswer(wrongAnswer);
            history.setCorrectAnswer(correctAnswer);
            history.setTimeSpent(timeSpent);
            history.setIsCorrect(isCorrect);
            mistakeHistoryRepository.save(history);

            log.info("记录答题历史：用户 {} 题目 {} 结果 {}", userId, questionId, isCorrect);
        }
    }

    /**
     * 更新错题的最后复习时间
     * 在复习模式下答题时调用
     *
     * @param questionId 题目ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLastReviewTime(Long questionId) {
        Long userId = securityUtils.getCurrentUserId();
        List<Mistake> mistakes = mistakeRepository.findByUserIdAndQuestionId(userId, questionId);

        if (!mistakes.isEmpty()) {
            Mistake mistake = mistakes.get(0);
            mistake.setLastMistakeTime(LocalDateTime.now());
            mistakeRepository.save(mistake);
        }
    }

    /**
     * 获取用户所有错题
     *
     * @return 错题列表
     */
    public List<QuizQuestion> getUserMistakes() {
        Long userId = securityUtils.getCurrentUserId();
        List<Mistake> mistakes = mistakeRepository.findByUserId(userId);

        if (mistakes.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取题目详情
        List<Long> questionIds = mistakes.stream()
                .map(Mistake::getQuestionId)
                .collect(Collectors.toList());
        List<QuizQuestion> questions = quizQuestionRepository.findAllById(questionIds);

        // 附加错题统计信息
        Map<Long, Mistake> mistakeMap = mistakes.stream()
                .collect(Collectors.toMap(Mistake::getQuestionId, m -> m));

        questions.forEach(q -> {
            Mistake m = mistakeMap.get(q.getId());
            if (m != null) {
                // 可以在 VO 中添加额外字段，这里暂时只返回题目
                q.setKnowledgePoint(q.getKnowledgePoint() + " (错误" + m.getMistakeCount() + "次)");
            }
        });

        return questions;
    }

    /**
     * 获取用户未掌握的错题
     *
     * @return 未掌握的错题列表
     */
    public List<Mistake> getUnmasteredMistakes() {
        Long userId = securityUtils.getCurrentUserId();
        return mistakeRepository.findUnmasteredByUserId(userId);
    }

    /**
     * 基于遗忘曲线推荐复习题目
     * Ebbinghaus 遗忘曲线：1天后、3天后、7天后、15天后、30天后
     *
     * @return 推荐复习的错题列表
     */
    public List<Mistake> getReviewRecommendation() {
        Long userId = securityUtils.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();

        // 计算时间区间
        LocalDateTime oneDayAgo = now.minusDays(1);
        LocalDateTime threeDaysAgo = now.minusDays(3);
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime fifteenDaysAgo = now.minusDays(15);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        // 获取需要复习的错题（按最近错误时间排序）
        // 这里简化处理，实际应该根据上次复习时间和错误次数计算优先级
        List<Mistake> unmastered = mistakeRepository.findUnmasteredByUserId(userId);

        // 过滤出需要复习的题目（基于上次错误时间）
        List<Mistake> toReview = unmastered.stream()
                .filter(m -> {
                    if (m.getLastMistakeTime() == null) return false;
                    long daysSinceMistake = java.time.Duration.between(m.getLastMistakeTime(), now).toDays();
                    return daysSinceMistake >= 1; // 至少1天后可以复习
                })
                .sorted((a, b) -> {
                    // 优先复习最早错误且未复习的题目
                    return a.getLastMistakeTime().compareTo(b.getLastMistakeTime());
                })
                .limit(10) // 每次最多推荐10题
                .collect(Collectors.toList());

        log.info("用户 {} 推荐复习 {} 道错题", userId, toReview.size());
        return toReview;
    }

    /**
     * 获取复习推荐列表（基于艾宾浩斯遗忘曲线）
     * 返回增强的VO，包含下次复习时间、优先级等信息
     *
     * @param userId 用户ID
     * @return 推荐列表
     */
    public List<ReviewRecommendationVO> getReviewRecommendations(Long userId) {
        // 1. 获取用户未掌握的错题
        List<Mistake> unmasteredMistakes = mistakeRepository.findUnmasteredByUserId(userId);

        if (unmasteredMistakes.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 过滤掉在延迟期内的错题
        LocalDateTime now = LocalDateTime.now();
        List<Mistake> activeMistakes = unmasteredMistakes.stream()
                .filter(m -> {
                        if (m.getSnoozedUntil() == null) return true;
                        return m.getSnoozedUntil().isBefore(now);
                })
                .toList();

        if (activeMistakes.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. 获取关联的题目信息
        List<Long> questionIds = activeMistakes.stream()
                .map(Mistake::getQuestionId)
                .distinct()
                .toList();

        List<QuizQuestion> questions = quizQuestionRepository.findAllById(questionIds);
        Map<Long, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        // 4. 计算推荐优先级（基于遗忘曲线）
        List<ReviewRecommendationVO> recommendations = new ArrayList<>();

        for (Mistake mistake : activeMistakes) {
            QuizQuestion question = questionMap.get(mistake.getQuestionId());
            if (question == null) continue;

            ReviewRecommendationVO vo = ReviewRecommendationVO.builder()
                            .mistakeId(mistake.getId())
                            .questionId(mistake.getQuestionId())
                            .questionText(question.getQuestionText())
                            .questionType(question.getQuestionType() != null ? question.getQuestionType().getCode() : null)
                            .knowledgePoint(question.getKnowledgePoint())
                            .blankCount(question.getBlankCount())
                            .build();

            // 计算下次复习时间（艾宾浩斯遗忘曲线）
            // 间隔：20分钟、1小时、8小时、1天、2天、6天、15天、30天
            int mistakeCount = mistake.getMistakeCount() != null ? mistake.getMistakeCount() : 1;
            LocalDateTime lastReviewTime = mistake.getLastMistakeTime() != null ?
                    mistake.getLastMistakeTime() : mistake.getCreatedTime();

            Duration interval = calculateReviewInterval(mistakeCount);
            LocalDateTime nextReviewTime = lastReviewTime.plus(interval);

            vo.setLastReviewTime(lastReviewTime);
            vo.setNextReviewDate(nextReviewTime);
            vo.setMistakeCount(mistake.getMistakeCount());

            // 计算紧急程度和优先级
            long daysUntilReview = java.time.temporal.ChronoUnit.DAYS.between(now, nextReviewTime);
            int priority = calculatePriority(daysUntilReview, mistakeCount);
            vo.setPriority(priority);

            recommendations.add(vo);
        }

        // 4. 按优先级降序排序（优先级高的在前，相同优先级按下次复习时间升序）
        recommendations.sort((a, b) -> {
            int priorityCompare = Integer.compare(
                b.getPriority() != null ? b.getPriority() : 0,
                a.getPriority() != null ? a.getPriority() : 0
            );
            if (priorityCompare != 0) {
                return priorityCompare;
            }
            // 优先级相同，按下次复习时间升序（快到期的在前）
            if (a.getNextReviewDate() != null && b.getNextReviewDate() != null) {
                return a.getNextReviewDate().compareTo(b.getNextReviewDate());
            }
            return 0;
        });

        // 5. 限制返回数量（最多20条）
        List<com.review.agent.entity.vo.ReviewRecommendationVO> result =
                recommendations.stream().limit(20).toList();

        log.info("用户 {} 复习推荐数量：{}", userId, result.size());
        return result;
    }

    /**
     * 计算复习间隔（艾宾浩斯遗忘曲线）
     */
    private java.time.Duration calculateReviewInterval(int mistakeCount) {
        // 间隔序列：20分钟、1小时、8小时、1天、2天、6天、15天、30天
        Duration[] intervals = {
                Duration.ofMinutes(20),
                Duration.ofHours(1),
                Duration.ofHours(8),
                Duration.ofDays(1),
                Duration.ofDays(2),
                Duration.ofDays(6),
                Duration.ofDays(15),
                Duration.ofDays(30)
        };

        int index = Math.min(mistakeCount - 1, intervals.length - 1);
        return intervals[Math.max(0, index)];
    }

    /**
     * 计算优先级
     * 优先级 = 紧急程度分 + 错误次数权重
     */
    private int calculatePriority(long daysUntilReview, int mistakeCount) {
        // 紧急程度：已逾期(100) -> 今天到期(80) -> 1天内(60) -> 3天内(40) -> 正常(20)
        int urgencyScore;
        if (daysUntilReview < 0) {
            urgencyScore = 100;
        } else if (daysUntilReview == 0) {
            urgencyScore = 80;
        } else if (daysUntilReview <= 1) {
            urgencyScore = 60;
        } else if (daysUntilReview <= 3) {
            urgencyScore = 40;
        } else {
            urgencyScore = 20;
        }

        // 错误次数权重：每错一次加5分，最多20分
        int mistakeCountWeight = Math.min(mistakeCount * 5, 20);

        return urgencyScore + mistakeCountWeight;
    }

    /**
     * 标记错题为已掌握
     * 通常在用户连续答对3次后调用
     *
     * @param questionId 题目ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsMastered(Long questionId) {
        Long userId = securityUtils.getCurrentUserId();
        List<Mistake> mistakes = mistakeRepository.findByUserIdAndQuestionId(userId, questionId);

        if (!mistakes.isEmpty()) {
            Mistake mistake = mistakes.get(0);
            mistake.setMastered(true);
            mistakeRepository.save(mistake);
            log.info("用户 {} 已掌握题目 {}", userId, questionId);
        }
    }

    /**
     * 稍后复习(延迟复习提醒)
     * 将错题的复习提醒延迟指定小时数,最多延迟3次
     *
     * @param mistakeId 错题ID
     * @param hours 延迟小时数(1-24小时)
     */
    @Transactional(rollbackFor = Exception.class)
    public void snoozeReview(Long mistakeId, Integer hours) {
        Long userId = securityUtils.getCurrentUserId();

        // 验证错题所有权
        Mistake mistake = mistakeRepository.findById(mistakeId)
                .orElseThrow(() -> new IllegalArgumentException("错题不存在"));

        if (!mistake.getUserId().equals(userId)) {
                throw new IllegalArgumentException("无权操作此错题");
        }

        // 检查延迟次数限制(最多3次)
        int snoozeCount = mistake.getSnoozeCount() != null ? mistake.getSnoozeCount() : 0;
        if (snoozeCount >= 3) {
                throw new IllegalArgumentException("延迟复习次数已达上限(3次)");
        }

        // 设置延迟时间
        LocalDateTime snoozedUntil = LocalDateTime.now().plusHours(hours);
        mistake.setSnoozedUntil(snoozedUntil);
        mistake.setSnoozeCount(snoozeCount + 1);
        mistakeRepository.save(mistake);

        log.info("用户 {} 延迟复习错题 {} {} 小时", userId, mistakeId, hours);
    }

    /**
     * 获取错题统计信息
     *
     * @return 统计Map：{total: 总数, unmastered: 未掌握, mastered: 已掌握}
     */
    public Map<String, Object> getMistakeStats() {
        Long userId = securityUtils.getCurrentUserId();
        long unmastered = mistakeRepository.countUnmasteredByUserId(userId);
        long mastered = mistakeRepository.countMasteredByUserId(userId);

        return Map.of(
                "total", unmastered + mastered,
                "unmastered", unmastered,
                "mastered", mastered
        );
    }

    /**
     * 获取错题列表（返回VO）
     *
     * @param filter 筛选条件：all(全部), unmastered(未掌握), mastered(已掌握)
     * @return 错题VO列表
     */
    public List<MistakeVo> getMistakeList(String filter) {
        Long userId = securityUtils.getCurrentUserId();
        List<Mistake> mistakes = mistakeRepository.findByUserId(userId);

        // 应用筛选条件
        if ("unmastered".equals(filter)) {
            mistakes = mistakes.stream()
                    .filter(m -> !m.getMastered())
                    .collect(Collectors.toList());
        } else if ("mastered".equals(filter)) {
            mistakes = mistakes.stream()
                    .filter(Mistake::getMastered)
                    .collect(Collectors.toList());
        }

        if (mistakes.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取题目详情
        List<Long> questionIds = mistakes.stream()
                .map(Mistake::getQuestionId)
                .collect(Collectors.toList());
        List<QuizQuestion> questions = quizQuestionRepository.findAllById(questionIds);

        // 构建题目映射
        Map<Long, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        // 转换为VO
        List<MistakeVo> result = mistakes.stream()
                .map(mistake -> {
                    QuizQuestion question = questionMap.get(mistake.getQuestionId());
                    if (question == null) {
                        return null;
                    }

                    return MistakeVo.builder()
                            .id(mistake.getId())
                            .questionId(mistake.getQuestionId())
                            .questionText(question.getQuestionText())
                            .questionType(question.getQuestionType() != null ? question.getQuestionType().name() : null)
                            .optionsJson(question.getOptionsJson())
                            .correctAnswer(question.getCorrectAnswer())
                            .explanation(question.getExplanation())
                            .knowledgePoint(question.getKnowledgePoint())
                            .mistakeCount(mistake.getMistakeCount())
                            .lastMistakeTime(mistake.getLastMistakeTime())
                            .mastered(mistake.getMastered())
                            .createdTime(mistake.getCreatedTime())
                            .blankCount(question.getBlankCount())
                            .build();
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());

        log.info("用户 {} 获取错题列表，筛选条件：{}，结果数量：{}", userId, filter, result.size());
        return result;
    }

    /**
     * 根据题目ID获取错题详情（精确查询）
     *
     * @param userId 用户ID
     * @param questionId 题目ID
     * @return 错题VO，如果不存在返回null
     */
    public MistakeVo getMistakeByQuestionId(Long userId, Long questionId) {
        List<Mistake> mistakes = mistakeRepository.findByUserIdAndQuestionId(userId, questionId);

        if (mistakes.isEmpty()) {
            return null;
        }

        Mistake mistake = mistakes.get(0);

        // 获取题目详情
        QuizQuestion question = quizQuestionRepository.findById(questionId).orElse(null);
        if (question == null) {
            return null;
        }

        // 转换为VO
        return MistakeVo.builder()
                .id(mistake.getId())
                .questionId(mistake.getQuestionId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType() != null ? question.getQuestionType().name() : null)
                .optionsJson(question.getOptionsJson())
                .correctAnswer(question.getCorrectAnswer())
                .explanation(question.getExplanation())
                .knowledgePoint(question.getKnowledgePoint())
                .mistakeCount(mistake.getMistakeCount())
                .lastMistakeTime(mistake.getLastMistakeTime())
                .mastered(mistake.getMastered())
                .createdTime(mistake.getCreatedTime())
                .blankCount(question.getBlankCount())
                .build();
    }


    /**
     * 批量标记错题为已掌握
     *
     * @param questionIds 题目ID列表
     * @return 成功标记的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchMarkMastered(List<Long> questionIds) {
        Long userId = securityUtils.getCurrentUserId();
        int count = 0;

        for (Long questionId : questionIds) {
            List<Mistake> mistakes = mistakeRepository.findByUserIdAndQuestionId(userId, questionId);
            if (!mistakes.isEmpty()) {
                Mistake mistake = mistakes.get(0);
                if (!mistake.getMastered()) {
                    mistake.setMastered(true);
                    mistakeRepository.save(mistake);
                    count++;
                }
            }
        }

        log.info("用户 {} 批量标记 {} 道错题为已掌握", userId, count);
        return count;
    }

    /**
     * 批量删除错题
     *
     * @param questionIds 题目ID列表
     * @return 成功删除的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchDelete(List<Long> questionIds) {
        Long userId = securityUtils.getCurrentUserId();
        int count = 0;

        for (Long questionId : questionIds) {
            List<Mistake> mistakes = mistakeRepository.findByUserIdAndQuestionId(userId, questionId);
            if (!mistakes.isEmpty()) {
                mistakeRepository.delete(mistakes.get(0));
                count++;
            }
        }

        log.info("用户 {} 批量删除 {} 道错题", userId, count);
        return count;
    }

    /**
     * 获取错题答题历史
     *
     * @param userId 用户ID
     * @param mistakeId 错题ID
     * @return 历史记录列表
     */
    public List<MistakeHistoryVO> getMistakeHistory(Long userId, Long mistakeId) {
        // 验证权限
        Mistake mistake = mistakeRepository.findById(mistakeId)
                .orElse(null);

        if (mistake == null || !mistake.getUserId().equals(userId)) {
            throw new IllegalArgumentException("错题不存在或无权访问");
        }

        // 查询历史记录
        List<MistakeHistory> historyList =
                mistakeHistoryRepository.findByMistakeIdOrderByCreatedTimeDesc(mistakeId);

        // 转换为VO
        return historyList.stream()
                .map(history -> MistakeHistoryVO.builder()
                        .id(history.getId())
                        .mistakeId(history.getMistakeId())
                        .wrongAnswer(history.getWrongAnswer())
                        .correctAnswer(history.getCorrectAnswer())
                        .timeSpent(history.getTimeSpent())
                        .isCorrect(history.getIsCorrect())
                        .createdTime(history.getCreatedTime())
                        .build())
                .toList();
    }
}
