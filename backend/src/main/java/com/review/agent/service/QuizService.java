package com.review.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.pojo.CollectionRelation;
import com.review.agent.entity.pojo.KnowledgeMastery;
import com.review.agent.entity.pojo.QuestionType;
import com.review.agent.entity.pojo.QuizQuestion;
import com.review.agent.entity.pojo.QuizRecord;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.CollectionRelationRepository;
import com.review.agent.repository.QuizQuestionRepository;
import com.review.agent.repository.QuizRecordRepository;
import com.review.agent.common.utils.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuizService {

    @Resource
    private QuizRecordRepository quizRecordRepository;

    @Resource
    private QuizQuestionRepository quizQuestionRepository;

    @Resource
    private CollectionRelationRepository collectionRelationRepository;

    @Resource
    private AnalysisResultRepository analysisResultRepository;

    @Resource
    private ChatClient analysisChatClient;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private MistakeBookService mistakeBookService;

    @Resource
    private KnowledgeMasteryService knowledgeMasteryService;

    @Transactional(rollbackFor = Exception.class)
    public QuizRecord generateQuiz(Long userId, Long collectionId) {
        // 0. Check if quiz already exists
        List<QuizRecord> existingRecords = quizRecordRepository.findByUserIdAndCollectionIdOrderByCreatedTimeDesc(userId, collectionId);
        if (!CollectionUtils.isEmpty(existingRecords)) {
            // Return the latest one
            return existingRecords.get(0);
        }

        // 1. Get analysis results from collection
        List<CollectionRelation> relations = collectionRelationRepository.findByCollectionId(collectionId);
        if (CollectionUtils.isEmpty(relations)) {
            throw new RuntimeException("Collection is empty");
        }
        
        List<Long> analysisIds = relations.stream().map(CollectionRelation::getAnalysisResultId).collect(Collectors.toList());
        List<AnalysisResult> analysisResults = analysisResultRepository.findAllById(analysisIds);
        
        if (CollectionUtils.isEmpty(analysisResults)) {
             throw new RuntimeException("No analysis results found");
        }

        // 2. Generate questions via LLM
        // Prepare context for LLM
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < analysisResults.size(); i++) {
            AnalysisResult ar = analysisResults.get(i);
            context.append(String.format("Case %d:\nProblem: %s\n",
                i + 1, ar.getProblemStatement()));
        }

        String prompt = String.format("""
            Based on the following code analysis cases, generate 3-5 questions to test the user's understanding.
            
            Mix different question types appropriately:
            - Single choice (single_choice): For basic concepts and principles
            - Multiple choice (multiple_choice): For multi-faceted issues
            - True/false (true_false): For concept clarification
            - Fill in blank (fill_blank): For key terms and parameters
            - Code snippet (code_snippet): For code analysis and debugging
            
            %s
            
            Return the result ONLY as a JSON array with the following format, And Using Chinese:
            [
              {
                "question": "Question text",
                "type": "single_choice|multiple_choice|true_false|fill_blank|code_snippet",
                "options": ["A. Option 1", "B. Option 2", "C. Option 3", "D. Option 4"],
                "answer": "A" or ["A","B"] for multiple choice,
                "explanation": "Detailed explanation",
                "knowledgePoint": "Key concept or topic (e.g., 'Java并发', 'Spring事务')",
                "difficulty": 1-5,
                "timeLimit": 30-120,
                "relatedCaseIndex": 1
              }
            ]
            """, context);

        String jsonResponse = analysisChatClient.prompt().user(prompt).call().content();
        // Clean up markdown code blocks if present
        if (jsonResponse.startsWith("```json")) {
            jsonResponse = jsonResponse.substring(7);
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            }
        }
        jsonResponse = jsonResponse.trim();

        // 3. Save Quiz Record
        QuizRecord record = new QuizRecord();
        record.setUserId(userId);
        record.setCollectionId(collectionId);
        record.setStatus(0); // In Progress
        record = quizRecordRepository.save(record);

        // 4. Save Questions
        try {
            List<Map<String, Object>> questions = objectMapper.readValue(jsonResponse, List.class);
            List<QuizQuestion> quizQuestions = new ArrayList<>();

            for (Map<String, Object> q : questions) {
                QuizQuestion qq = new QuizQuestion();
                quizQuestions.add(qq);
                qq.setQuizId(record.getId());
                qq.setQuestionText((String) q.get("question"));

                // Handle answer for different question types
                Object answerObj = q.get("answer");
                if (answerObj instanceof String) {
                    qq.setCorrectAnswer((String) answerObj);
                } else if (answerObj instanceof List) {
                    // Multiple choice: join with comma
                    List<?> answers = (List<?>) answerObj;
                    qq.setCorrectAnswer(answers.stream().map(Object::toString).collect(Collectors.joining(",")));
                }

                // Handle options (may be array or null for fill_blank/true_false)
                Object optionsObj = q.get("options");
                if (optionsObj != null) {
                    qq.setOptionsJson(objectMapper.writeValueAsString(optionsObj));
                }

                qq.setExplanation((String) q.get("explanation"));

                // Parse question type
                String typeStr = (String) q.getOrDefault("type", "single_choice");
                try {
                    qq.setQuestionType(QuestionType.fromCode(typeStr));
                } catch (Exception e) {
                    qq.setQuestionType(QuestionType.SINGLE_CHOICE);
                }

                // Parse difficulty level
                Object difficultyObj = q.get("difficulty");
                if (difficultyObj instanceof Number) {
                    qq.setDifficultyLevel(((Number) difficultyObj).intValue());
                }

                // Parse knowledge point
                String knowledgePoint = (String) q.get("knowledgePoint");
                if (knowledgePoint != null && !knowledgePoint.trim().isEmpty()) {
                    qq.setKnowledgePoint(knowledgePoint);
                } else {
                    // Extract from problem statement if not provided
                    qq.setKnowledgePoint(extractKnowledgePoint((String) q.get("question")));
                }

                // Parse time limit
                Object timeLimitObj = q.get("timeLimit");
                if (timeLimitObj instanceof Number) {
                    qq.setTimeLimit(((Number) timeLimitObj).intValue());
                }

                // Try to link back to original analysis result
                Object caseIndexObj = q.get("relatedCaseIndex");
                if (caseIndexObj instanceof Integer) {
                    int idx = (Integer) caseIndexObj - 1;
                    if (idx >= 0 && idx < analysisResults.size()) {
                        qq.setRelatedAnalysisId(analysisResults.get(idx).getId());
                    }
                }
            }
            quizQuestionRepository.saveAll(quizQuestions);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response for quiz", e);
            throw new RuntimeException("Failed to generate quiz");
        }

        return record;
    }

    public List<QuizQuestion> getQuizQuestions(Long quizId) {
        return quizQuestionRepository.findByQuizId(quizId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void submitAnswer(Long questionId, String userAnswer) {
        QuizQuestion question = quizQuestionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Check answer correctness based on question type
        boolean isCorrect = checkAnswer(question, userAnswer);

        // Update question statistics
        question.setUserAnswer(userAnswer);
        question.setIsCorrect(isCorrect);
        question.setAnswerCount(question.getAnswerCount() + 1);
        if (isCorrect) {
            question.setCorrectCount(question.getCorrectCount() + 1);
        }
        quizQuestionRepository.save(question);

        // Record in mistake book if incorrect
        if (!isCorrect) {
            mistakeBookService.recordAnswer(questionId, false, question.getQuizId());
        }

        // Update knowledge mastery
        if (question.getKnowledgePoint() != null) {
            knowledgeMasteryService.updateMastery(question.getKnowledgePoint(), isCorrect, null);
        }
    }

    /**
     * Check answer correctness based on question type
     *
     * @param question Question object
     * @param userAnswer User's answer
     * @return Whether the answer is correct
     */
    private boolean checkAnswer(QuizQuestion question, String userAnswer) {
        if (userAnswer == null) return false;

        String correctAnswer = question.getCorrectAnswer();
        QuestionType type = question.getQuestionType();

        switch (type) {
            case MULTIPLE_CHOICE:
                // Multiple choice: compare as sets (order doesn't matter)
                List<String> userOptions = Arrays.asList(userAnswer.split(","));
                List<String> correctOptions = Arrays.asList(correctAnswer.split(","));
                return userOptions.size() == correctOptions.size()
                        && userOptions.containsAll(correctOptions);

            case TRUE_FALSE:
                // True/false: case-insensitive comparison
                return userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());

            case FILL_BLANK:
                // Fill blank: allow partial match (case-insensitive)
                return userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());

            case SINGLE_CHOICE:
            case CODE_SNIPPET:
            default:
                // Single choice and code snippet: exact match (case-insensitive)
                return userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
        }
    }

    /**
     * Extract knowledge point from question text (simple heuristic)
     *
     * @param questionText Question text
     * @return Extracted knowledge point
     */
    private String extractKnowledgePoint(String questionText) {
        // Simple keyword extraction - can be enhanced with NLP
        if (questionText == null || questionText.isEmpty()) {
            return "通用知识";
        }

        // Look for common technical terms
        String[] keywords = {"Java", "Spring", "并发", "事务", "数据库", "SQL", "Redis",
                          "线程", "锁", "异步", "Stream", "Lambda", "泛型"};

        for (String keyword : keywords) {
            if (questionText.contains(keyword)) {
                return keyword;
            }
        }

        return "通用知识";
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetQuiz(Long quizId) {
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizId(quizId);
        if (CollectionUtils.isEmpty(questions)) {
            return;
        }
        for (QuizQuestion q : questions) {
            q.setUserAnswer(null);
            q.setIsCorrect(null);
        }
        quizQuestionRepository.saveAll(questions);
    }
}
