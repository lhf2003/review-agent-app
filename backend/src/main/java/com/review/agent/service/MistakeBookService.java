package com.review.agent.service;

import com.review.agent.entity.pojo.Mistake;
import com.review.agent.entity.pojo.QuizQuestion;
import com.review.agent.entity.vo.MistakeVo;
import com.review.agent.repository.MistakeRepository;
import com.review.agent.repository.QuizQuestionRepository;
import com.review.agent.common.utils.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                // 这里需要额外的字段来跟踪连续正确次数，暂时简化处理
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
                            .build();
                })
                .filter(vo -> vo != null)
                .collect(Collectors.toList());

        log.info("用户 {} 获取错题列表，筛选条件：{}，结果数量：{}", userId, filter, result.size());
        return result;
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
}
