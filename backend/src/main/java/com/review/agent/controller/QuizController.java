package com.review.agent.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.QuizQuestion;
import com.review.agent.entity.pojo.QuizRecord;
import com.review.agent.entity.request.BatchSubmitRequest;
import com.review.agent.entity.request.CollectionRequest;
import com.review.agent.entity.vo.QuizResultSummary;
import com.review.agent.entity.vo.QuizVo;
import com.review.agent.service.QuizService;
import jakarta.annotation.Resource;
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
}
