package com.review.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.pojo.CollectionRelation;
import com.review.agent.entity.pojo.QuizQuestion;
import com.review.agent.entity.pojo.QuizRecord;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.CollectionRelationRepository;
import com.review.agent.repository.QuizQuestionRepository;
import com.review.agent.repository.QuizRecordRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
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
            Based on the following code analysis cases, generate 3-5 multiple-choice questions to test the user's understanding of these issues.
            
            %s
            
            Return the result ONLY as a JSON array with the following format, And Using Chinese:
            [
              {
                "question": "Question text",
                "options": ["A. Option 1", "B. Option 2", "C. Option 3", "D. Option 4"],
                "answer": "A",
                "explanation": "Why A is correct...",
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
                qq.setOptionsJson(objectMapper.writeValueAsString(q.get("options")));
                qq.setCorrectAnswer((String) q.get("answer"));
                qq.setExplanation((String) q.get("explanation"));
                
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
        
        question.setUserAnswer(userAnswer);
        question.setIsCorrect(question.getCorrectAnswer().equalsIgnoreCase(userAnswer));
        quizQuestionRepository.save(question);
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
