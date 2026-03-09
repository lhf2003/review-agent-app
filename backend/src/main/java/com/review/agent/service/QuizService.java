package com.review.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.entity.pojo.AnalysisCollection;
import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.pojo.CollectionRelation;
import com.review.agent.common.enums.QuestionType;
import com.review.agent.entity.pojo.QuizQuestion;
import com.review.agent.entity.pojo.QuizRecord;
import com.review.agent.entity.pojo.Tag;
import com.review.agent.entity.request.BatchSubmitRequest;
import com.review.agent.entity.vo.*;
import com.review.agent.repository.AnalysisCollectionRepository;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.CollectionRelationRepository;
import com.review.agent.repository.QuizQuestionRepository;
import com.review.agent.repository.QuizRecordRepository;
import com.review.agent.repository.TagDimensionRepository;
import com.review.agent.repository.TagRepository;
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

import java.time.LocalDateTime;
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
    private TagService tagService;

    @Resource
    private TagDimensionRepository tagDimensionRepository;

    @Resource
    private TagRepository tagRepository;

    @Resource
    private KnowledgeMasteryService knowledgeMasteryService;

    @Resource
    private SecurityUtils securityUtils;

    @Transactional(rollbackFor = Exception.class)
    public QuizRecord generateQuiz(Long userId, Long collectionId) {
        // 0. Check if quiz already exists
        QuizRecord existingQuiz = findExistingQuiz(userId, collectionId);
        if (existingQuiz != null) {
            return existingQuiz;
        }

        // 1. Get analysis results from collection
        List<AnalysisResult> analysisResults = fetchAnalysisResults(collectionId);

        // 2. Generate questions via LLM
        String context = buildAnalysisContext(analysisResults);
        String jsonResponse = callLLMForQuestions(context);

        // 3. Save Quiz Record
        QuizRecord record = createQuizRecord(userId, collectionId);

        // 4. Parse and save questions
        parseAndSaveQuestions(jsonResponse, record, analysisResults);

        return record;
    }

    /**
     * 查找用户已有的测验记录
     *
     * @param userId 用户ID
     * @param collectionId 合集ID
     * @return 最新的测验记录，如果没有则返回null
     */
    private QuizRecord findExistingQuiz(Long userId, Long collectionId) {
        List<QuizRecord> existingRecords = quizRecordRepository.findByUserIdAndCollectionIdOrderByCreatedTimeDesc(userId, collectionId);
        if (!CollectionUtils.isEmpty(existingRecords)) {
            return existingRecords.get(0);
        }
        return null;
    }

    /**
     * 获取合集关联的分析结果
     *
     * @param collectionId 合集ID
     * @return 分析结果列表
     */
    private List<AnalysisResult> fetchAnalysisResults(Long collectionId) {
        List<CollectionRelation> relations = collectionRelationRepository.findByCollectionId(collectionId);
        if (CollectionUtils.isEmpty(relations)) {
            throw new RuntimeException("Collection is empty");
        }

        List<Long> analysisIds = relations.stream()
            .map(CollectionRelation::getAnalysisResultId)
            .collect(Collectors.toList());
        List<AnalysisResult> analysisResults = analysisResultRepository.findAllById(analysisIds);

        if (CollectionUtils.isEmpty(analysisResults)) {
            throw new RuntimeException("No analysis results found");
        }

        return analysisResults;
    }

    /**
     * 构建LLM上下文
     *
     * @param analysisResults 分析结果列表
     * @return 格式化的上下文字符串
     */
    private String buildAnalysisContext(List<AnalysisResult> analysisResults) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < analysisResults.size(); i++) {
            AnalysisResult ar = analysisResults.get(i);
            context.append(String.format("Case %d:\nProblem: %s\n", i + 1, ar.getProblemStatement()));
        }
        return context.toString();
    }

    /**
     * 调用LLM生成题目
     *
     * @param context 上下文内容
     * @return LLM返回的JSON字符串
     */
    private String callLLMForQuestions(String context) {
        Long userId = securityUtils.getCurrentUserId();

        String tagStructure = buildTagStructureForPrompt(userId);

        String prompt = promptService.getQuizGenerationPrompt(context, tagStructure);
        String jsonResponse = analysisChatClient.prompt().user(prompt).call().content();
        return cleanJsonResponse(jsonResponse);
    }

    /**
     * 构建标签结构字符串用于Prompt
     */
    private String buildTagStructureForPrompt(Long userId) {
        try {
            // 获取技术领域维度ID
            var techDomain = tagDimensionRepository.findByCode("TECH_DOMAIN")
                    .orElse(null);
            if (techDomain == null) {
                return "暂无标签结构";
            }

            // 获取标签树
            List<TagService.TagVO> tagTree = tagService.getTagTree(userId, techDomain.getId());

            // 格式化为文本结构
            StringBuilder sb = new StringBuilder();
            for (TagService.TagVO root : tagTree) {
                appendTagTree(sb, root, 0);
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to build tag structure for prompt", e);
            return "暂无标签结构";
        }
    }

    private void appendTagTree(StringBuilder sb, TagService.TagVO tag, int level) {
        String indent = "  ".repeat(level);
        sb.append(indent).append("- ").append(tag.getName());
        if (tag.getPath() != null) {
            sb.append(" (路径: ").append(tag.getPath()).append(")");
        }
        sb.append("\n");

        if (tag.getChildren() != null) {
            for (TagService.TagVO child : tag.getChildren()) {
                appendTagTree(sb, child, level + 1);
            }
        }
    }

    /**
     * 清理LLM返回的JSON响应（移除markdown代码块）
     *
     * @param jsonResponse 原始响应
     * @return 清理后的JSON字符串
     */
    private String cleanJsonResponse(String jsonResponse) {
        if (jsonResponse.startsWith("```json")) {
            jsonResponse = jsonResponse.substring(7);
            if (jsonResponse.endsWith("```")) {
                jsonResponse = jsonResponse.substring(0, jsonResponse.length() - 3);
            }
        }
        return jsonResponse.trim();
    }

    /**
     * 创建并保存测验记录
     *
     * @param userId 用户ID
     * @param collectionId 合集ID
     * @return 保存后的测验记录
     */
    private QuizRecord createQuizRecord(Long userId, Long collectionId) {
        QuizRecord record = new QuizRecord();
        record.setUserId(userId);
        record.setCollectionId(collectionId);
        record.setStatus(0);
        record.setCollectionVersionHash(generateCollectionVersionHash(collectionId));
        return quizRecordRepository.save(record);
    }

    /**
     * 解析LLM返回的JSON并保存题目
     *
     * @param jsonResponse LLM返回的JSON
     * @param record 测验记录
     * @param analysisResults 分析结果列表（用于关联）
     * @return 保存的题目列表
     */
    private List<QuizQuestion> parseAndSaveQuestions(String jsonResponse, QuizRecord record, List<AnalysisResult> analysisResults) {
        try {
            List<Map<String, Object>> questions = objectMapper.readValue(jsonResponse, List.class);
            List<QuizQuestion> quizQuestions = new ArrayList<>();

            for (Map<String, Object> q : questions) {
                QuizQuestion qq = parseQuestion(q, record, analysisResults);
                quizQuestions.add(qq);
            }

            return quizQuestionRepository.saveAll(quizQuestions);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse LLM response for quiz", e);
            throw new RuntimeException("Failed to generate quiz");
        }
    }

    /**
     * 解析单道题目
     *
     * @param q 题目数据Map
     * @param record 测验记录
     * @param analysisResults 分析结果列表
     * @return 解析后的题目对象
     */
    private QuizQuestion parseQuestion(Map<String, Object> q, QuizRecord record, List<AnalysisResult> analysisResults) {
        QuizQuestion qq = new QuizQuestion();
        qq.setQuizId(record.getId());
        qq.setQuestionText((String) q.get("question"));

        // Handle answer for different question types
        parseAndSetAnswer(qq, q.get("answer"));

        // Handle options
        parseAndSetOptions(qq, q.get("options"));

        qq.setExplanation((String) q.get("explanation"));

        // Parse question type
        parseAndSetQuestionType(qq, (String) q.getOrDefault("type", "single_choice"));

        // Parse difficulty level
        parseAndSetDifficulty(qq, q.get("difficulty"));

        // Parse knowledge point
        parseAndSetKnowledgePoint(qq, q);

        // Parse time limit
        parseAndSetTimeLimit(qq, q.get("timeLimit"));

        // Parse blank count
        parseAndSetBlankCount(qq, q);

        // Link to analysis result
        linkToAnalysisResult(qq, q.get("relatedCaseIndex"), analysisResults);

        return qq;
    }

    /**
     * 解析并设置答案
     */
    private void parseAndSetAnswer(QuizQuestion qq, Object answerObj) {
        if (answerObj instanceof String) {
            qq.setCorrectAnswer((String) answerObj);
        } else if (answerObj instanceof List) {
            List<?> answers = (List<?>) answerObj;
            qq.setCorrectAnswer(answers.stream().map(Object::toString).collect(Collectors.joining(",")));
        }
    }

    /**
     * 解析并设置选项
     */
    private void parseAndSetOptions(QuizQuestion qq, Object optionsObj) {
        if (optionsObj != null) {
            try {
                qq.setOptionsJson(objectMapper.writeValueAsString(optionsObj));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize options, using empty array", e);
                qq.setOptionsJson("[]");
            }
        } else {
            qq.setOptionsJson("[]");
        }
    }

    /**
     * 解析并设置题目类型
     */
    private void parseAndSetQuestionType(QuizQuestion qq, String typeStr) {
        try {
            qq.setQuestionType(QuestionType.fromCode(typeStr));
        } catch (Exception e) {
            qq.setQuestionType(QuestionType.SINGLE_CHOICE);
        }
    }

    /**
     * 解析并设置难度等级
     */
    private void parseAndSetDifficulty(QuizQuestion qq, Object difficultyObj) {
        if (difficultyObj instanceof Number) {
            qq.setDifficultyLevel(((Number) difficultyObj).intValue());
        }
    }

    /**
     * 解析并设置知识点（支持标签路径匹配）
     */
    private void parseAndSetKnowledgePoint(QuizQuestion qq, Map<String, Object> q) {
        String knowledgePoint = (String) q.get("knowledgePoint");

        if (knowledgePoint != null && !knowledgePoint.trim().isEmpty()) {
            // LLM 成功匹配到标签路径
            qq.setKnowledgePoint(knowledgePoint);

            // 尝试关联到具体标签ID（根据路径查找）
            Long tagId = findTagIdByPath(knowledgePoint);
            if (tagId != null) {
                qq.setTagId(tagId);
            }
        } else {
            // 无法匹配，使用启发式提取作为后备
            String extracted = extractKnowledgePoint((String) q.get("question"));
            qq.setKnowledgePoint(extracted);
        }
    }

    /**
     * 根据标签路径查找标签ID
     */
    private Long findTagIdByPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            return tagRepository.findByPath(path)
                    .map(Tag::getId)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to find tag by path: {}", path, e);
            return null;
        }
    }

    /**
     * 解析并设置时间限制
     */
    private void parseAndSetTimeLimit(QuizQuestion qq, Object timeLimitObj) {
        if (timeLimitObj instanceof Number) {
            qq.setTimeLimit(((Number) timeLimitObj).intValue());
        }
    }

    /**
     * 解析并设置填空数量
     */
    private void parseAndSetBlankCount(QuizQuestion qq, Map<String, Object> q) {
        Object blankCountObj = q.get("blankCount");
        if (blankCountObj instanceof Number) {
            qq.setBlankCount(((Number) blankCountObj).intValue());
        } else if (qq.getQuestionType() == QuestionType.FILL_BLANK) {
            String questionText = (String) q.get("question");
            if (questionText != null) {
                qq.setBlankCount(countBlanks(questionText));
            }
        }
    }

    /**
     * 关联到分析结果
     */
    private void linkToAnalysisResult(QuizQuestion qq, Object caseIndexObj, List<AnalysisResult> analysisResults) {
        if (caseIndexObj instanceof Integer) {
            int idx = (Integer) caseIndexObj - 1;
            if (idx >= 0 && idx < analysisResults.size()) {
                qq.setRelatedAnalysisId(analysisResults.get(idx).getId());
            }
        }
    }

    public List<QuizQuestion> getQuizQuestions(Long quizId) {
        return quizQuestionRepository.findByQuizId(quizId);
    }

    @Transactional(rollbackFor = Exception.class)
    public SubmitAnswerResultVO submitAnswer(Long questionId, String userAnswer, Boolean reviewMode) {
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

        // Record answer history (包含详细答题信息)
        String wrongAnswer = isCorrect ? null : userAnswer;
        String correctAnswer = question.getCorrectAnswer();
        mistakeBookService.recordAnswerHistory(
                questionId,
                isCorrect,
                wrongAnswer,
                correctAnswer,
                null, // timeSpent 暂时为 null，因为单个提交没有记录时间
                question.getQuizId()
        );

        // 如果是复习模式提交，更新错题的最后复习时间
        if (Boolean.TRUE.equals(reviewMode)) {
            mistakeBookService.updateLastReviewTime(questionId);
        }

        // Update knowledge mastery
        if (question.getKnowledgePoint() != null) {
            knowledgeMasteryService.updateMastery(question.getKnowledgePoint(), isCorrect, null);
        }

        // Return result
        return new SubmitAnswerResultVO(
                isCorrect,
                userAnswer,
                correctAnswer,
                question.getExplanation(),
                question.getKnowledgePoint()
        );
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
                }

                // 记录答题历史（包含详细答题信息）
                String wrongAnswer = isCorrect ? null : qa.getUserAnswer();
                String correctAnswer = question.getCorrectAnswer();
                mistakeBookService.recordAnswerHistory(
                        qa.getQuestionId(),
                        isCorrect,
                        wrongAnswer,
                        correctAnswer,
                        qa.getTimeSpent(), // 从请求中获取答题用时
                        quizId
                );

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

        // 设置做题时间（用户提交答案的时间）
        quizRecord.setSubmitTime(LocalDateTime.now());

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
                return userOptions.size() == correctOptions.size() && userOptions.containsAll(correctOptions);
            case TRUE_FALSE:
                return  userAnswer.trim().equalsIgnoreCase(correctAnswer.trim());

            case FILL_BLANK:
                // Fill blank: split by comma/semicolon, compare each blank case-insensitively
                String[] userBlanks = userAnswer.split("[,;]");
                String[] correctBlanks = correctAnswer.split("[,;]");

                if (userBlanks.length != correctBlanks.length) {
                    return false;
                }

                for (int i = 0; i < userBlanks.length; i++) {
                    if (!userBlanks[i].trim().equalsIgnoreCase(correctBlanks[i].trim())) {
                        return false;
                    }
                }
                return true;

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
        List<CollectionRelation> relations = collectionRelationRepository.findByCollectionId(collectionId);

        List<Long> analysisIds = relations.stream()
            .map(CollectionRelation::getAnalysisResultId)
            .sorted()  // 关键：排序确保顺序一致
            .toList();

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

        List<QuizRecord> existingRecords = quizRecordRepository.findByUserIdAndCollectionIdOrderByCreatedTimeDesc(
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
                "### Case %d:\n**问题**: %s\n**方案**: %s\n\n",
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

        // 批量预加载，解决 N+1 查询问题
        List<QuizRecord> quizzes = quizPage.getContent();
        if (quizzes.isEmpty()) {
            return quizPage.map(quiz -> new QuizHistoryVO());
        }

        // 1. 批量获取所有 quizId
        List<Long> quizIds = quizzes.stream()
            .map(QuizRecord::getId)
            .collect(Collectors.toList());

        // 2. 批量获取所有 collectionId
        List<Long> collectionIds = quizzes.stream()
            .map(QuizRecord::getCollectionId)
            .distinct()
            .collect(Collectors.toList());

        // 3. 批量查询合集信息
        Map<Long, String> collectionNameMap = analysisCollectionRepository
            .findByIdIn(collectionIds)
            .stream()
            .collect(Collectors.toMap(
                AnalysisCollection::getId,
                AnalysisCollection::getName
            ));

        // 4. 批量查询所有题目
        List<QuizQuestion> allQuestions = quizQuestionRepository.findByQuizIdIn(quizIds);

        // 5. 按 quizId 分组统计
        Map<Long, List<QuizQuestion>> questionsByQuizId = allQuestions.stream()
            .collect(Collectors.groupingBy(QuizQuestion::getQuizId));

        // 6. 构建结果
        return quizPage.map(quiz -> {
            QuizHistoryVO vo = new QuizHistoryVO();
            vo.setQuizId(quiz.getId());
            vo.setCollectionId(quiz.getCollectionId());
            vo.setCollectionName(collectionNameMap.get(quiz.getCollectionId()));
            vo.setTotalScore(quiz.getTotalScore());
            vo.setStatus(quiz.getStatus());
            vo.setIsOutdated(quiz.getIsOutdated());
            vo.setCreatedTime(quiz.getCreatedTime());

            // 从预加载的数据中获取题目统计
            List<QuizQuestion> questions = questionsByQuizId.getOrDefault(quiz.getId(), Collections.emptyList());
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
        List<QuizRecord> quizRecords = quizRecordRepository.findAllByUserIdAndStatusOrderBySubmitTimeAsc(userId, 1);

        return quizRecords.stream()
            .map(record -> {
                // 使用做题时间（submitTime）而不是创建时间
                LocalDateTime timeToUse = record.getSubmitTime() != null ? record.getSubmitTime() : record.getCreatedTime();
                return QuizStatsVO.QuizScoreTrendVo.builder()
                    .date(timeToUse.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .score(record.getTotalScore())
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取用户知识点掌握度
     *
     * @param userId 用户ID
     * @return 知识点掌握度列表（按正确率降序）
     */
    private List<QuizStatsVO.KnowledgeMasteryVo> getKnowledgeMastery(Long userId) {
        // 查询用户所有已完成测验的问题（按做题时间排序）
        List<QuizRecord> quizRecords = quizRecordRepository.findAllByUserIdAndStatusOrderBySubmitTimeAsc(userId, 1);

        if (quizRecords.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量获取所有 quizId
        List<Long> quizIds = quizRecords.stream()
            .map(QuizRecord::getId)
            .collect(Collectors.toList());

        // 批量查询所有题目，解决 N+1 问题
        List<QuizQuestion> allQuestions = quizQuestionRepository.findByQuizIdIn(quizIds);

        Map<String, Integer> tagCorrectCount = new HashMap<>();
        Map<String, Integer> tagTotalCount = new HashMap<>();

        for (QuizQuestion question : allQuestions) {
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

    // ==================== 学习仪表盘相关 ====================

    /**
     * 获取学习仪表盘数据
     *
     * @param userId 用户ID
     * @return 学习仪表盘数据
     */
    public LearningDashboardVO getLearningDashboard(Long userId) {
        // 1. 获取测验分数趋势（最近30天）
        List<LearningDashboardVO.ScoreTrendItem> scoreTrend = getScoreTrend(userId, 30);

        // 2. 获取知识点雷达图数据
        List<LearningDashboardVO.KnowledgeRadarItem> knowledgeRadar = getKnowledgeRadar(userId);

        // 3. 获取学习热力图数据（最近12个月）
        Map<String, Integer> heatmap = getLearningHeatmap(userId);

        // 4. 获取学习时长分布
        Map<String, Integer> timeDistribution = getTimeDistribution(userId);

        // 5. 获取周统计数据
        LearningDashboardVO.WeeklyStats weeklyStats = getWeeklyStats(userId);

        return LearningDashboardVO.builder()
            .scoreTrend(scoreTrend)
            .knowledgeRadar(knowledgeRadar)
            .heatmap(heatmap)
            .timeDistribution(timeDistribution)
            .weeklyStats(weeklyStats)
            .build();
    }

    /**
     * 按时间范围获取测验分数趋势
     *
     * @param userId 用户ID
     * @param range 时间范围（天数）
     * @return 分数趋势列表
     */
    public List<LearningDashboardVO.ScoreTrendItem> getScoreTrend(Long userId, int range) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(range);
        LocalDateTime endTime = LocalDateTime.now();

        List<QuizRecord> quizRecords = quizRecordRepository.findByUserIdAndSubmitTimeRange(userId, startTime, endTime);

        if (quizRecords.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量获取合集名称
        List<Long> collectionIds = quizRecords.stream()
            .map(QuizRecord::getCollectionId)
            .distinct()
            .collect(Collectors.toList());

        Map<Long, String> collectionNameMap = analysisCollectionRepository.findByIdIn(collectionIds)
            .stream()
            .collect(Collectors.toMap(
                AnalysisCollection::getId,
                AnalysisCollection::getName
            ));

        return quizRecords.stream()
            .map(record -> {
                // 使用做题时间（submitTime）而不是创建时间
                LocalDateTime timeToUse = record.getSubmitTime() != null ? record.getSubmitTime() : record.getCreatedTime();
                return LearningDashboardVO.ScoreTrendItem.builder()
                    .date(timeToUse)
                    .score(record.getTotalScore())
                    .quizId(record.getId())
                    .collectionName(collectionNameMap.get(record.getCollectionId()))
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * 获取知识点雷达图数据（最多8个知识点）
     *
     * @param userId 用户ID
     * @return 雷达图数据列表
     */
    private List<LearningDashboardVO.KnowledgeRadarItem> getKnowledgeRadar(Long userId) {
        // 从 KnowledgeMastery 表获取数据（返回VO格式）
        List<KnowledgeMasteryVO> masteries = knowledgeMasteryService.getWeakKnowledgePoints(userId, 100);

        // 过滤答题数量 >= 3 的知识点，按答题数量排序取前8个
        return masteries.stream()
            .filter(km -> km.getTotalCount() >= 3)
            .sorted((a, b) -> Integer.compare(b.getTotalCount(), a.getTotalCount()))
            .limit(8)
            .map(km -> LearningDashboardVO.KnowledgeRadarItem.builder()
                .name(km.getKnowledgePoint())
                .value(km.getMasteryRate())
                .totalCount(km.getTotalCount())
                .build())
            .collect(Collectors.toList());
    }

    /**
     * 获取学习热力图数据（最近12个月）
     *
     * @param userId 用户ID
     * @return 日期 -> 测验数量映射
     */
    private Map<String, Integer> getLearningHeatmap(Long userId) {
        LocalDateTime startTime = LocalDateTime.now().minusMonths(12);
        List<QuizRecord> records = quizRecordRepository.findRecentYearBySubmitTimeRecords(userId, startTime);

        Map<String, Integer> heatmap = new HashMap<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (QuizRecord record : records) {
            // 使用做题时间（submitTime）而不是创建时间
            LocalDateTime timeToUse = record.getSubmitTime() != null ? record.getSubmitTime() : record.getCreatedTime();
            String dateKey = timeToUse.format(formatter);
            heatmap.put(dateKey, heatmap.getOrDefault(dateKey, 0) + 1);
        }

        return heatmap;
    }

    /**
     * 获取学习时长分布
     *
     * @param userId 用户ID
     * @return 时段 -> 测验数量映射
     */
    private Map<String, Integer> getTimeDistribution(Long userId) {
        LocalDateTime startTime = LocalDateTime.now().minusMonths(3); // 最近3个月
        List<QuizRecord> records = quizRecordRepository.findRecentYearBySubmitTimeRecords(userId, startTime);

        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("凌晨", 0);
        distribution.put("上午", 0);
        distribution.put("下午", 0);
        distribution.put("晚上", 0);

        for (QuizRecord record : records) {
            // 使用做题时间（submitTime）而不是创建时间
            LocalDateTime timeToUse = record.getSubmitTime() != null ? record.getSubmitTime() : record.getCreatedTime();
            int hour = timeToUse.getHour();
            String period = getTimePeriod(hour);
            distribution.put(period, distribution.get(period) + 1);
        }

        return distribution;
    }

    /**
     * 根据小时数获取时段名称
     *
     * @param hour 小时数（0-23）
     * @return 时段名称
     */
    private String getTimePeriod(int hour) {
        if (hour >= 0 && hour < 6) {
            return "凌晨";
        } else if (hour >= 6 && hour < 12) {
            return "上午";
        } else if (hour >= 12 && hour < 18) {
            return "下午";
        } else {
            return "晚上";
        }
    }

    /**
     * 获取周统计数据（本周 vs 上周对比）
     *
     * @param userId 用户ID
     * @return 周统计数据
     */
    private LearningDashboardVO.WeeklyStats getWeeklyStats(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        // 本周开始（周一）
        LocalDateTime thisWeekStart = now.with(java.time.DayOfWeek.MONDAY).withHour(0).withMinute(0).withSecond(0);
        // 上周开始
        LocalDateTime lastWeekStart = thisWeekStart.minusWeeks(1);

        // 获取本周测验（按做题时间）
        List<QuizRecord> thisWeekRecords = quizRecordRepository.findByUserIdAndSubmitTimeRange(userId, thisWeekStart, now);
        // 获取上周测验（按做题时间）
        List<QuizRecord> lastWeekRecords = quizRecordRepository.findByUserIdAndSubmitTimeRange(userId, lastWeekStart, thisWeekStart);

        // 计算本周统计
        int thisWeekQuizCount = thisWeekRecords.size();
        double thisWeekAccuracy = calculateAverageAccuracy(thisWeekRecords);

        // 计算上周统计
        int lastWeekQuizCount = lastWeekRecords.size();
        double lastWeekAccuracy = calculateAverageAccuracy(lastWeekRecords);

        // 计算变化
        int quizCountChange = thisWeekQuizCount - lastWeekQuizCount;
        double accuracyChange = thisWeekAccuracy - lastWeekAccuracy;

        return LearningDashboardVO.WeeklyStats.builder()
            .thisWeekQuizCount(thisWeekQuizCount)
            .lastWeekQuizCount(lastWeekQuizCount)
            .thisWeekAccuracy(thisWeekAccuracy)
            .lastWeekAccuracy(lastWeekAccuracy)
            .quizCountChange(quizCountChange)
            .accuracyChange(Math.round(accuracyChange * 100.0) / 100.0)
            .build();
    }

    /**
     * 计算测验列表的平均正确率
     *
     * @param records 测验记录列表
     * @return 平均正确率
     */
    private double calculateAverageAccuracy(List<QuizRecord> records) {
        if (records.isEmpty()) {
            return 0.0;
        }

        double totalScore = records.stream()
            .filter(r -> r.getTotalScore() != null)
            .mapToInt(QuizRecord::getTotalScore)
            .average()
            .orElse(0.0);

        return Math.round(totalScore * 100.0) / 100.0;
    }
}
