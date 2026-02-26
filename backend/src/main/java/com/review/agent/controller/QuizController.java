package com.review.agent.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.QuizQuestion;
import com.review.agent.entity.pojo.QuizRecord;
import com.review.agent.entity.request.BatchSubmitRequest;
import com.review.agent.entity.request.ResetQuizRequest;
import com.review.agent.entity.request.SubmitAnswerRequest;
import com.review.agent.entity.vo.*;
import com.review.agent.service.KnowledgeMasteryService;
import com.review.agent.service.QuizService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    @Resource
    private QuizService quizService;

    @Resource
    private KnowledgeMasteryService knowledgeMasteryService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private SecurityUtils securityUtils;

    /**
     * 为指定合集生成测验
     *
     * @return 生成的测验
     */
    @PostMapping("/collection/{collectionId}/generate")
    public BaseResponse<QuizVo> generateQuiz(@PathVariable Long collectionId) {
        Long userId = securityUtils.getCurrentUserId();
        QuizRecord record = quizService.generateQuiz(userId, collectionId);
        List<QuizQuestion> questions = quizService.getQuizQuestions(record.getId());

        QuizVo vo = new QuizVo();
        vo.setId(record.getId());
        vo.setCollectionId(collectionId);

        List<QuizVo.QuestionVo> qVos = new ArrayList<>();
        for (QuizQuestion q : questions) {
            QuizVo.QuestionVo qVo = new QuizVo.QuestionVo();
            qVo.setId(q.getId());
            qVo.setQuestion(q.getQuestionText());
            qVo.setType(q.getQuestionType() != null ? q.getQuestionType().getCode() : "single_choice");
            qVo.setAnswer(q.getCorrectAnswer());
            qVo.setExplanation(q.getExplanation());
            qVo.setUserAnswer(q.getUserAnswer());
            qVo.setBlankCount(q.getBlankCount());
            qVo.setKnowledgePoint(q.getKnowledgePoint());
            try {
                qVo.setOptions(objectMapper.readValue(q.getOptionsJson(), List.class));
            } catch (JsonProcessingException e) {
                qVo.setOptions(new ArrayList<>());
            }
            qVos.add(qVo);
        }
        vo.setQuestions(qVos);

        return ResultUtil.success(vo);
    }

    /**
     * 提交单个答案
     *
     * @param request 包含 questionId 和 userAnswer
     * @return 答题结果（包含是否正确、正确答案、解析等）
     */
    @PostMapping("/submit-answer")
    public BaseResponse<SubmitAnswerResultVO> submitAnswer(@Valid @RequestBody SubmitAnswerRequest request) {
        SubmitAnswerResultVO result = quizService.submitAnswer(
                request.getQuestionId(),
                request.getUserAnswer(),
                request.getReviewMode()
        );
        return ResultUtil.success(result);
    }

    /**
     * 批量提交答案
     *
     * @param request 批量提交请求
     * @return 答题结果统计
     */
    @PostMapping("/submit-batch-answers")
    public BaseResponse<QuizResultSummary> submitBatchAnswers(@RequestBody BatchSubmitRequest request) {
        QuizResultSummary summary = quizService.submitBatchAnswers(
                request.getQuizId(),
                request.getAnswers()
        );
        return ResultUtil.success(summary);
    }

    /**
     * 重置测验
     *
     * @param request 包含 quizId
     * @return 操作结果
     */
    @PostMapping("/reset")
    public BaseResponse<Void> resetQuiz(@Valid @RequestBody ResetQuizRequest request) {
        quizService.resetQuiz(request.getQuizId());
        return ResultUtil.success();
    }

    /**
     * 查询用户习题历史列表
     *
     * @param status       状态筛选（null=全部，0=进行中，1=已完成）
     * @param collectionId 合集筛选（null=全部合集）
     * @param page         页码（从0开始）
     * @param size         每页大小
     * @return 分页的习题历史
     */
    @GetMapping("/history")
    public BaseResponse<Page<QuizHistoryVO>> getQuizHistory(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long collectionId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        Long userId = securityUtils.getCurrentUserId();
        Page<QuizHistoryVO> history = quizService.getQuizHistory(
                userId, status, collectionId, page, size
        );
        return ResultUtil.success(history);
    }

    /**
     * 获取习题详情
     *
     * @param quizId 习题ID
     * @return 习题详情
     */
    @GetMapping("/{quizId}/detail")
    public BaseResponse<QuizDetailVO> getQuizDetail(@PathVariable Long quizId) {
        Long userId = securityUtils.getCurrentUserId();
        QuizDetailVO detail = quizService.getQuizDetail(userId, quizId);
        return ResultUtil.success(detail);
    }

    /**
     * 检测题库版本是否与合集版本一致
     *
     * @param collectionId 合集ID
     * @return 版本检测结果
     */
    @GetMapping("/collection/{collectionId}/version-check")
    public BaseResponse<QuizVersionCheckResult> checkQuizVersion(@PathVariable Long collectionId) {
        Long userId = securityUtils.getCurrentUserId();
        QuizVersionCheckResult result = quizService.checkQuizVersion(userId, collectionId);
        return ResultUtil.success(result);
    }

    /**
     * 重新生成题库（基于旧题知识点+新内容）
     *
     * @param collectionId 合集ID
     * @return 新生成的QuizRecord
     */
    @PostMapping("/collection/{collectionId}/regenerate")
    public BaseResponse<QuizRecord> regenerateQuiz(
            @PathVariable Long collectionId
    ) {
        Long userId = securityUtils.getCurrentUserId();
        QuizRecord newQuiz = quizService.regenerateQuizWithNewContent(userId, collectionId);
        return ResultUtil.success(newQuiz);
    }

    /**
     * 获取习题统计数据
     *
     * @return 习题统计数据（分数趋势和知识点掌握度）
     */
    @GetMapping("/stats")
    public BaseResponse<QuizStatsVO> getQuizStats() {
        Long userId = securityUtils.getCurrentUserId();
        QuizStatsVO stats = quizService.getQuizStats(userId);
        return ResultUtil.success(stats);
    }

    /**
     * 获取用户的知识点掌握度列表
     *
     * @param limit 限制数量
     * @return 掌握度列表
     */
    @GetMapping("/knowledge-mastery")
    public BaseResponse<List<KnowledgeMasteryVO>> getKnowledgeMastery(
            @RequestParam(defaultValue = "20") int limit
    ) {
        Long userId = securityUtils.getCurrentUserId();
        List<KnowledgeMasteryVO> mastery = knowledgeMasteryService.getUserKnowledgeMastery(userId, limit);
        return ResultUtil.success(mastery);
    }

    // ==================== 学习仪表盘 API ====================

    /**
     * 获取学习仪表盘数据
     *
     * @return 学习仪表盘数据（分数趋势、知识点雷达图、热力图、时间分布、周统计）
     */
    @GetMapping("/dashboard")
    public BaseResponse<LearningDashboardVO> getLearningDashboard() {
        Long userId = securityUtils.getCurrentUserId();
        LearningDashboardVO dashboard = quizService.getLearningDashboard(userId);
        return ResultUtil.success(dashboard);
    }

    /**
     * 按时间范围获取测验分数趋势
     *
     * @param range 时间范围（7/30/90/all 天）
     * @return 分数趋势列表
     */
    @GetMapping("/dashboard/trend")
    public BaseResponse<List<LearningDashboardVO.ScoreTrendItem>> getScoreTrend(
            @RequestParam(defaultValue = "30") String range
    ) {
        Long userId = securityUtils.getCurrentUserId();
        int days = parseRangeToDays(range);
        List<LearningDashboardVO.ScoreTrendItem> trend = quizService.getScoreTrend(userId, days);
        return ResultUtil.success(trend);
    }

    /**
     * 解析时间范围参数为天数
     *
     * @param range 时间范围字符串
     * @return 天数
     */
    private int parseRangeToDays(String range) {
        return switch (range.toLowerCase()) {
            case "7" -> 7;
            case "30" -> 30;
            case "90" -> 90;
            case "all" -> 365 * 2; // 最近2年
            default -> 30;
        };
    }
}
