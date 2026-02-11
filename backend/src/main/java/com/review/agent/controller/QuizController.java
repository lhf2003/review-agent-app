package com.review.agent.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.QuizQuestion;
import com.review.agent.entity.pojo.QuizRecord;
import com.review.agent.entity.request.BatchSubmitRequest;
import com.review.agent.entity.vo.*;
import com.review.agent.service.KnowledgeMasteryService;
import com.review.agent.service.QuizService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/collection")
public class QuizController {

    @Resource
    private QuizService quizService;

    @Resource
    private KnowledgeMasteryService knowledgeMasteryService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private SecurityUtils securityUtils;

    @PostMapping("/generate-quiz")
    public BaseResponse<QuizVo> generateQuiz(@RequestBody Map<String, Long> body) {
        Long collectionId = body.get("collectionId");
        if (collectionId == null) {
            return ResultUtil.error("collectionId is required");
        }

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
            qVo.setType(q.getQuestionType() != null ? q.getQuestionType().getCode() : "single_choice"); // 添加题目类型
            qVo.setAnswer(q.getCorrectAnswer());
            qVo.setExplanation(q.getExplanation());
            qVo.setUserAnswer(q.getUserAnswer());
            qVo.setBlankCount(q.getBlankCount()); // 添加填空题的空位数量
            qVo.setKnowledgePoint(q.getKnowledgePoint()); // 添加知识点
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

    @PostMapping("/submit-answer")
    public BaseResponse<Void> submitAnswer(@RequestBody Map<String, Object> body) {
        Long questionId = Long.valueOf(body.get("questionId").toString());
        String userAnswer = (String) body.get("userAnswer");

        quizService.submitAnswer(questionId, userAnswer);
        return ResultUtil.success();
    }

    /**
     * 批量提交答案
     * @param request 批量提交请求
     * @return 答题结果统计
     */
    @PostMapping("/submit-batch-answers")
    public BaseResponse<QuizResultSummary> submitBatchAnswers(@RequestBody BatchSubmitRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        QuizResultSummary summary = quizService.submitBatchAnswers(
                request.getQuizId(),
                request.getAnswers()
        );
        return ResultUtil.success(summary);
    }

    @PostMapping("/reset")
    public BaseResponse<Void> resetQuiz(@RequestBody Map<String, Object> body) {
        Long quizId = Long.valueOf(body.get("quizId").toString());
        quizService.resetQuiz(quizId);
        return ResultUtil.success();
    }

    /**
     * 查询用户习题历史列表
     *
     * @param status 状态筛选（null=全部，0=进行中，1=已完成）
     * @param collectionId 合集筛选（null=全部合集）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页的习题历史
     */
    @GetMapping("/quiz/history")
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
    @GetMapping("/quiz/{quizId}/detail")
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
    @GetMapping("/{collectionId}/quiz/version-check")
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
    @PostMapping("/{collectionId}/quiz/regenerate")
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
    @GetMapping("/quiz/stats")
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
    @GetMapping("/quiz/knowledge-mastery")
    public BaseResponse<List<KnowledgeMasteryVO>> getKnowledgeMastery(
        @RequestParam(defaultValue = "20") int limit
    ) {
        Long userId = securityUtils.getCurrentUserId();
        List<KnowledgeMasteryVO> mastery = knowledgeMasteryService.getUserKnowledgeMastery(userId, limit);
        return ResultUtil.success(mastery);
    }
}
