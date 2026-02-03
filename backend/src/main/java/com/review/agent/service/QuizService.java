package com.review.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.entity.pojo.AnalysisCollection;
import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.pojo.CollectionRelation;
import com.review.agent.entity.pojo.KnowledgeMastery;
import com.review.agent.entity.pojo.QuestionType;
import com.review.agent.entity.pojo.QuizQuestion;
import com.review.agent.entity.pojo.QuizRecord;
import com.review.agent.entity.request.BatchSubmitRequest;
import com.review.agent.entity.vo.*;
import com.review.agent.repository.AnalysisCollectionRepository;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.CollectionRelationRepository;
import com.review.agent.repository.QuizQuestionRepository;
import com.review.agent.repository.QuizRecordRepository;
import com.review.agent.common.utils.SecurityUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

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
    private AnalysisCollectionRepository analysisCollectionRepository;

    @Resource
    private ChatClient analysisChatClient;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private PromptService promptService;

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

        // Use PromptService to get the optimized prompt
        String prompt = promptService.getQuizGenerationPrompt(context.toString());

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
                } else {
                    // For question types without options (e.g., fill_blank), set empty array
                    qq.setOptionsJson("[]");
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

                // Parse blank count (for fill_blank questions)
                Object blankCountObj = q.get("blankCount");
                if (blankCountObj instanceof Number) {
                    qq.setBlankCount(((Number) blankCountObj).intValue());
                } else if (qq.getQuestionType() == QuestionType.FILL_BLANK) {
                    // For fill_blank questions without blankCount, calculate from question text
                    String questionText = (String) q.get("question");
                    if (questionText != null) {
                        int blankCount = countBlanks(questionText);
                        qq.setBlankCount(blankCount);
                    }
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
     * 批量提交答案
     *
     * @param quizId 测验记录ID
     * @param answers 答案列表
     * @return 答题结果统计
     */
    @Transactional(rollbackFor = Exception.class)
    public QuizResultSummary submitBatchAnswers(Long quizId, List<BatchSubmitRequest.QuestionAnswer> answers) {
        // 1. 验证 quizId
        QuizRecord quizRecord = quizRecordRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz record not found"));

        // 2. 验证 answers 不为空
        if (answers == null || answers.isEmpty()) {
            throw new RuntimeException("Answers list is empty");
        }

        // 3. 初始化统计对象
        QuizResultSummary summary = new QuizResultSummary();
        summary.setTotalCount(answers.size());

        int correct = 0;
        int incorrect = 0;
        int unanswered = 0;

        // 4. 遍历提交每个答案
        for (BatchSubmitRequest.QuestionAnswer qa : answers) {
            try {
                QuizQuestion question = quizQuestionRepository.findById(qa.getQuestionId())
                        .orElseThrow(() -> new RuntimeException("Question not found: " + qa.getQuestionId()));

                // 判断是否未作答
                if (qa.getUserAnswer() == null || qa.getUserAnswer().trim().isEmpty()) {
                    unanswered++;
                    continue;
                }

                // 判断答案正确性
                boolean isCorrect = checkAnswer(question, qa.getUserAnswer());

                // 更新题目数据
                question.setUserAnswer(qa.getUserAnswer());
                question.setIsCorrect(isCorrect);
                question.setAnswerCount(question.getAnswerCount() + 1);

                if (isCorrect) {
                    question.setCorrectCount(question.getCorrectCount() + 1);
                    correct++;
                } else {
                    incorrect++;
                    // 记录到错题本
                    mistakeBookService.recordAnswer(qa.getQuestionId(), false, quizId);
                }

                // 更新知识点掌握度
                if (question.getKnowledgePoint() != null) {
                    knowledgeMasteryService.updateMastery(question.getKnowledgePoint(), isCorrect, null);
                }

                quizQuestionRepository.save(question);

            } catch (Exception e) {
                log.error("Failed to submit answer for questionId: {}", qa.getQuestionId(), e);
                // 继续处理其他题目，不中断整个流程
                unanswered++;
            }
        }

        // 5. 更新测验记录状态为已完成
        quizRecord.setStatus(1); // Completed

        // 计算总分（百分制）
        int totalQuestions = answers.size();
        double scorePercent = totalQuestions > 0 ? (correct * 100.0 / totalQuestions) : 0;
        quizRecord.setTotalScore((int) Math.round(scorePercent));

        // 保存测验记录
        quizRecordRepository.save(quizRecord);

        // 6. 设置统计数据
        summary.setCorrectCount(correct);
        summary.setIncorrectCount(incorrect);
        summary.setUnansweredCount(unanswered);

        log.info("Quiz {} completed with score: {}", quizId, quizRecord.getTotalScore());
        return summary;
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
                return  userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());

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

        // TODO Look for common technical terms
        String[] keywords = {"Java", "Spring", "并发", "事务", "数据库", "SQL", "Redis",
                "线程", "锁", "异步", "Stream", "Lambda", "泛型"};

        for (String keyword : keywords) {
            if (questionText.contains(keyword)) {
                return keyword;
            }
        }

        return "通用知识";
    }

    /**
     * Count the number of blanks in a fill-in-the-blank question
     *
     * @param questionText Question text
     * @return Number of blanks (at least 1)
     */
    private int countBlanks(String questionText) {
        if (questionText == null || questionText.isEmpty()) {
            return 1;
        }

        // Count consecutive underscores (e.g., "_____" counts as 1 blank)
        int count = 0;
        boolean inBlank = false;
        for (int i = 0; i < questionText.length(); i++) {
            char c = questionText.charAt(i);
            if (c == '_') {
                if (!inBlank) {
                    count++;
                    inBlank = true;
                }
            } else {
                inBlank = false;
            }
        }

        return Math.max(count / 5, 1);
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

    /**
     * 生成合集的版本哈希
     * 基于合集关联的分析结果ID列表排序后生成MD5哈希
     *
     * @param collectionId 合集ID
     * @return MD5哈希字符串
     */
    private String generateCollectionVersionHash(Long collectionId) {
        List<CollectionRelation> relations =
            collectionRelationRepository.findByCollectionId(collectionId);

        List<Long> analysisIds = relations.stream()
            .map(CollectionRelation::getAnalysisResultId)
            .sorted()  // 关键：排序确保顺序一致
            .collect(Collectors.toList());

        String content = analysisIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        return DigestUtils.md5DigestAsHex(content.getBytes());
    }

    /**
     * 检查题库版本是否与合集版本一致
     * 用于检测合集内容更新后，题库是否需要重新生成
     *
     * @param userId 用户ID
     * @param collectionId 合集ID
     * @return 版本检测结果
     */
    public QuizVersionCheckResult checkQuizVersion(Long userId, Long collectionId) {
        String currentHash = generateCollectionVersionHash(collectionId);

        List<QuizRecord> existingRecords =
            quizRecordRepository.findByUserIdAndCollectionIdOrderByCreatedTimeDesc(
                userId, collectionId
            );

        if (CollectionUtils.isEmpty(existingRecords)) {
            return QuizVersionCheckResult.builder()
                .hasExistingQuiz(false)
                .needsCreation(true)
                .currentHash(currentHash)
                .build();
        }

        QuizRecord latestRecord = existingRecords.get(0);
        String storedHash = latestRecord.getCollectionVersionHash();

        boolean isVersionMatch = currentHash.equals(storedHash);

        return QuizVersionCheckResult.builder()
            .hasExistingQuiz(true)
            .quizId(latestRecord.getId())
            .needsCreation(false)
            .isVersionMatch(isVersionMatch)
            .isOutdated(latestRecord.getIsOutdated())
            .currentHash(currentHash)
            .storedHash(storedHash)
            .build();
    }

    /**
     * 基于旧题知识点和新内容重新生成题库
     * 当合集新增分析结果后，使用此方法重新生成包含新旧内容的完整题库
     *
     * @param userId 用户ID
     * @param collectionId 合集ID
     * @return 新生成的QuizRecord
     */
    @Transactional(rollbackFor = Exception.class)
    public QuizRecord regenerateQuizWithNewContent(Long userId, Long collectionId) {
        // 1. 获取旧题库并提取知识点
        List<QuizRecord> oldRecords =
            quizRecordRepository.findByUserIdAndCollectionIdOrderByCreatedTimeDesc(
                userId, collectionId
            );

        Map<String, Integer> knowledgePointDifficulty = new HashMap<>();
        if (!CollectionUtils.isEmpty(oldRecords)) {
            QuizRecord oldRecord = oldRecords.get(0);
            List<QuizQuestion> oldQuestions =
                quizQuestionRepository.findTop10ByQuizId(oldRecord.getId());
            for (QuizQuestion q : oldQuestions) {
                String kp = q.getKnowledgePoint();
                int difficulty = q.getDifficultyLevel() != null ? q.getDifficultyLevel() : 3;
                knowledgePointDifficulty.put(kp, difficulty);
            }
        }

        // 2. 获取合集的所有分析结果
        List<CollectionRelation> relations =
            collectionRelationRepository.findByCollectionId(collectionId);
        List<Long> analysisIds = relations.stream()
            .map(CollectionRelation::getAnalysisResultId)
            .collect(Collectors.toList());
        List<AnalysisResult> analysisResults =
            analysisResultRepository.findAllById(analysisIds);

        if (CollectionUtils.isEmpty(analysisResults)) {
            throw new RuntimeException("No analysis results found");
        }

        // 3. 构建提示词
        StringBuilder knowledgePointsText = new StringBuilder();
        for (Map.Entry<String, Integer> entry : knowledgePointDifficulty.entrySet()) {
            knowledgePointsText.append(String.format("- %s（难度等级: %d）\n",
                entry.getKey(), entry.getValue()));
        }

        StringBuilder newCasesText = new StringBuilder();
        for (int i = 0; i < analysisResults.size(); i++) {
            AnalysisResult ar = analysisResults.get(i);
            newCasesText.append(String.format(
                "### Case %d:\n**问题**: %ss\n**方案**: %s\n\n",
                i + 1,
                ar.getProblemStatement(),
                ar.getSolution()
            ));
        }

        // 4. 调用LLM生成题目
        String prompt = promptService.getQuizRegenerationPrompt(
            knowledgePointsText.toString(),
            newCasesText.toString()
        );
        String jsonResponse = analysisChatClient.prompt().user(prompt).call().content();

        // 清理markdown代码块
        if (jsonResponse.startsWith("```json")) {
            jsonResponse = jsonResponse.substring(7);
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            }
        }
        jsonResponse = jsonResponse.trim();

        // 5. 保存新的QuizRecord
        QuizRecord newRecord = new QuizRecord();
        newRecord.setUserId(userId);
        newRecord.setCollectionId(collectionId);
        newRecord.setStatus(0);
        newRecord.setCollectionVersionHash(generateCollectionVersionHash(collectionId));
        newRecord.setIsOutdated(false);
        newRecord = quizRecordRepository.save(newRecord);

        // 6. 标记旧记录为过期
        if (!CollectionUtils.isEmpty(oldRecords)) {
            oldRecords.get(0).setIsOutdated(true);
            quizRecordRepository.save(oldRecords.get(0));
        }

        // 7. 保存新题目
        List<QuizQuestion> newQuestions = parseAndSaveQuestions(
            jsonResponse, newRecord, analysisResults
        );

        log.info("Regenerated quiz {} for collection {} with {} questions",
            newRecord.getId(), collectionId, newQuestions.size());
        return newRecord;
    }

    /**
     * 解析并保存题目
     *
     * @param jsonResponse LLM返回的JSON响应
     * @param record QuizRecord对象
     * @param analysisResults 分析结果列表
     * @return 保存的题目列表
     */
    private List<QuizQuestion> parseAndSaveQuestions(
        String jsonResponse, QuizRecord record, List<AnalysisResult> analysisResults
    ) {
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

                // Handle options
                Object optionsObj = q.get("options");
                if (optionsObj != null) {
                    qq.setOptionsJson(objectMapper.writeValueAsString(optionsObj));
                } else {
                    qq.setOptionsJson("[]");
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
                    qq.setKnowledgePoint(extractKnowledgePoint((String) q.get("question")));
                }

                // Parse time limit
                Object timeLimitObj = q.get("timeLimit");
                if (timeLimitObj instanceof Number) {
                    qq.setTimeLimit(((Number) timeLimitObj).intValue());
                }

                // Parse blank count
                Object blankCountObj = q.get("blankCount");
                if (blankCountObj instanceof Number) {
                    qq.setBlankCount(((Number) blankCountObj).intValue());
                } else if (qq.getQuestionType() == QuestionType.FILL_BLANK) {
                    String questionText = (String) q.get("question");
                    if (questionText != null) {
                        int blankCount = countBlanks(questionText);
                        qq.setBlankCount(blankCount);
                    }
                }

                // Link to analysis result
                Object caseIndexObj = q.get("relatedCaseIndex");
                if (caseIndexObj instanceof Integer) {
                    int idx = (Integer) caseIndexObj - 1;
                    if (idx >= 0 && idx < analysisResults.size()) {
                        qq.setRelatedAnalysisId(analysisResults.get(idx).getId());
                    }
                }
            }
            return quizQuestionRepository.saveAll(quizQuestions);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response for quiz regeneration", e);
            throw new RuntimeException("Failed to regenerate quiz");
        }
    }

    /**
     * 获取用户的习题历史（带筛选和分页）
     *
     * @param userId 用户ID
     * @param status 状态筛选（null=全部，0=进行中，1=已完成）
     * @param collectionId 合集筛选（null=全部合集）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页的习题历史
     */
    public Page<QuizHistoryVO> getQuizHistory(
        Long userId, Integer status, Long collectionId, Integer page, Integer size
    ) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by(Sort.Direction.DESC, "createdTime"));

        Page<QuizRecord> quizPage = quizRecordRepository.findByUserIdWithFilters(
            userId, status, collectionId, pageable
        );

        return quizPage.map(quiz -> {
            QuizHistoryVO vo = new QuizHistoryVO();
            vo.setQuizId(quiz.getId());
            vo.setCollectionId(quiz.getCollectionId());

            // 获取合集名称
            analysisCollectionRepository.findById(quiz.getCollectionId())
                .ifPresent(ac -> vo.setCollectionName(ac.getName()));

            vo.setTotalScore(quiz.getTotalScore());
            vo.setStatus(quiz.getStatus());
            vo.setIsOutdated(quiz.getIsOutdated());
            vo.setCreatedTime(quiz.getCreatedTime());

            // 统计题目数量和正确数量
            List<QuizQuestion> questions = quizQuestionRepository.findByQuizId(quiz.getId());
            vo.setQuestionCount(questions.size());
            vo.setCorrectCount((int) questions.stream()
                .filter(q -> Boolean.TRUE.equals(q.getIsCorrect()))
                .count());

            return vo;
        });
    }

    /**
     * 获取习题详情
     *
     * @param userId 用户ID
     * @param quizId 习题ID
     * @return 习题详情
     */
    public QuizDetailVO getQuizDetail(Long userId, Long quizId) {
        QuizRecord quiz = quizRecordRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));

        if (!quiz.getUserId().equals(userId)) {
            throw new RuntimeException("No permission");
        }

        QuizDetailVO vo = new QuizDetailVO();
        vo.setQuizId(quiz.getId());
        vo.setTotalScore(quiz.getTotalScore());
        vo.setStatus(quiz.getStatus());
        vo.setIsOutdated(quiz.getIsOutdated());
        vo.setCreatedTime(quiz.getCreatedTime());

        // 获取合集名称
        analysisCollectionRepository.findById(quiz.getCollectionId())
            .ifPresent(ac -> vo.setCollectionName(ac.getName()));

        // 获取所有题目详情
        List<QuizQuestion> questions = quizQuestionRepository.findByQuizId(quizId);
        List<QuestionDetailVO> questionDetails = questions.stream()
            .map(this::convertToQuestionDetailVO)
            .collect(Collectors.toList());

        vo.setQuestions(questionDetails);
        return vo;
    }

    /**
     * 将QuizQuestion转换为QuestionDetailVO
     *
     * @param question QuizQuestion对象
     * @return QuestionDetailVO对象
     */
    private QuestionDetailVO convertToQuestionDetailVO(QuizQuestion question) {
        return QuestionDetailVO.builder()
            .questionId(question.getId())
            .questionText(question.getQuestionText())
            .questionType(question.getQuestionType())
            .optionsJson(question.getOptionsJson())
            .correctAnswer(question.getCorrectAnswer())
            .userAnswer(question.getUserAnswer())
            .isCorrect(question.getIsCorrect())
            .explanation(question.getExplanation())
            .knowledgePoint(question.getKnowledgePoint())
            .difficultyLevel(question.getDifficultyLevel())
            .timeLimit(question.getTimeLimit())
            .blankCount(question.getBlankCount())
            .relatedAnalysisId(question.getRelatedAnalysisId())
            .build();
    }

    // ==================== 习题统计相关 ====================

    /**
     * 获取习题统计数据
     *
     * @param userId 用户ID
     * @return 习题统计数据
     */
    public QuizStatsVO getQuizStats(Long userId) {
        List<QuizStatsVO.QuizScoreTrendVo> scoreTrend = getQuizScoreTrend(userId);
        List<QuizStatsVO.KnowledgeMasteryVo> knowledgeMastery = getKnowledgeMastery(userId);

        return QuizStatsVO.builder()
            .quizScoreTrend(scoreTrend)
            .knowledgeMastery(knowledgeMastery)
            .build();
    }

    /**
     * 获取用户测验分数趋势
     *
     * @param userId 用户ID
     * @return 测验分数趋势列表（按时间升序）
     */
    private List<QuizStatsVO.QuizScoreTrendVo> getQuizScoreTrend(Long userId) {
        List<QuizRecord> quizRecords = quizRecordRepository.findAllByUserIdAndStatusOrderByCreatedTimeAsc(userId, 1);

        return quizRecords.stream()
            .map(record -> QuizStatsVO.QuizScoreTrendVo.builder()
                .date(record.getCreatedTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .score(record.getTotalScore())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * 获取用户知识点掌握度
     *
     * @param userId 用户ID
     * @return 知识点掌握度列表（按正确率降序）
     */
    private List<QuizStatsVO.KnowledgeMasteryVo> getKnowledgeMastery(Long userId) {
        // 查询用户所有已完成测验的问题
        List<QuizRecord> quizRecords = quizRecordRepository.findAllByUserIdAndStatusOrderByCreatedTimeAsc(userId, 1);
        Map<String, Integer> tagCorrectCount = new HashMap<>();
        Map<String, Integer> tagTotalCount = new HashMap<>();

        for (QuizRecord record : quizRecords) {
            List<QuizQuestion> questions = quizQuestionRepository.findByQuizId(record.getId());
            for (QuizQuestion question : questions) {
                // 获取知识点标签
                String knowledgePoint = question.getKnowledgePoint();
                if (knowledgePoint == null || knowledgePoint.trim().isEmpty()) {
                    continue; // 跳过没有标签的问题
                }

                // 统计该知识点下的题目总数
                tagTotalCount.put(knowledgePoint, tagTotalCount.getOrDefault(knowledgePoint, 0) + 1);

                // 统计正确数
                Boolean isCorrect = question.getIsCorrect();
                if (isCorrect != null && isCorrect) {
                    tagCorrectCount.put(knowledgePoint, tagCorrectCount.getOrDefault(knowledgePoint, 0) + 1);
                }
            }
        }

        // 计算每个标签的正确率
        List<QuizStatsVO.KnowledgeMasteryVo> masteryList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tagCorrectCount.entrySet()) {
            String tagName = entry.getKey();
            int correctCount = entry.getValue();
            int totalCount = tagTotalCount.getOrDefault(tagName, 0);
            if (totalCount > 0) {
                masteryList.add(QuizStatsVO.KnowledgeMasteryVo.builder()
                    .tagName(tagName)
                    .accuracyRate(correctCount * 100.0 / totalCount)
                    .build());
            }
        }

        // 按正确率降序排序
        masteryList.sort((a, b) -> Double.compare(b.getAccuracyRate(), a.getAccuracyRate()));

        return masteryList;
    }
}
