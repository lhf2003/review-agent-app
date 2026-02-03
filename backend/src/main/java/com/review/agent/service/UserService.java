package com.review.agent.service;

import com.review.agent.common.utils.ExceptionUtils;
import com.review.agent.common.utils.ObjectTransformUtil;
import com.review.agent.entity.pojo.*;
import com.review.agent.entity.request.BasicConfigUpdateRequest;
import com.review.agent.entity.request.UpdatePasswordRequest;
import com.review.agent.entity.vo.UserStatsVo;
import com.review.agent.repository.*;
import com.review.agent.schedule.DynamicScheduledService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private UserInfoRepository userInfoRepository;

    @Resource
    private UserConfigRepository userConfigRepository;

    @Resource
    private SelectedModelRepository selectedModelRepository;

    @Resource
    private UserLlmConfigRepository userLlmConfigRepository;

    @Resource
    private DefaultLlmProviderRepository defaultLlmProviderRepository;

    @Resource
    private UserDefaultModelConfigRepository userDefaultModelConfigRepository;

    @Resource
    @Lazy
    private DynamicScheduledService dynamicScheduledService;

    @Resource
    private DataInfoRepository dataInfoRepository;

    @Resource
    private AnalysisResultRepository analysisResultRepository;

    @Resource
    private AnalysisCollectionRepository analysisCollectionRepository;

    @Resource
    private MainTagRepository mainTagRepository;

    @Resource
    private SubTagRepository subTagRepository;

    @Resource
    private QuizRecordRepository quizRecordRepository;

    @Resource
    private SyncRecordRepository syncRecordRepository;

    @Resource
    private ReportDataRepository reportDataRepository;

    @Resource
    private AchievementDefinitionRepository achievementDefinitionRepository;

    @Resource
    private UserAchievementRepository userAchievementRepository;

    @Resource
    private QuizQuestionRepository quizQuestionRepository;

    // region 用户信息相关

    /**
     * 注册用户
     * @param userInfo 用户信息
     */
    @Transactional
    public void register(UserInfo userInfo) {
        String username = userInfo.getUsername();
        UserInfo userInfoFromDb = userInfoRepository.findByUsername(username);
        if (userInfoFromDb != null) {
            ExceptionUtils.throwDataInUse("user " + username + " already exists");
        }
        String password = userInfo.getPassword();
        // 密码加密（使用 BCrypt）
        String handledPassword = passwordEncoder.encode(password);
        userInfo.setPassword(handledPassword);
        Date date = new Date();
        userInfo.setCreateTime(date);
        userInfo.setUpdateTime(date);
        userInfo.setAvatar("http://cdn.meetfei.cn/default-img/java-logo.png");

        userInfoRepository.save(userInfo);


        // 初始化用户配置
        UserConfig userConfig = new UserConfig();
        userConfig.setUserId(userInfo.getId());
        // windows用户默认扫描目录
        userConfig.setScanDirectory("C:\\Users\\" + username + "\\Desktop");
        userConfig.setAutoScanEnabled(true);
        userConfig.setScanIntervalSeconds(600);
        userConfig.setWeeklyEnabled(false);
        userConfig.setDailyEnabled(false);
        userConfig.setUpdateTime(date);

        userConfigRepository.save(userConfig);

        addDefaultLlmConfig(userInfo.getId());
        initializeUserAchievements(userInfo.getId());
    }

    /**
     * 为新用户分配默认模型提供商
     * @param userId 用户 id
     */
    private void addDefaultLlmConfig(Long userId) {
        List<DefaultLlmProvider> llmProviderList = defaultLlmProviderRepository.findAll();
        List<UserLlmConfig> userLlmConfigList = new ArrayList<>();
        for (DefaultLlmProvider defaultLlmProvider : llmProviderList) {
            UserLlmConfig userLlmConfig = new UserLlmConfig();
            userLlmConfig.setUserId(userId);
            userLlmConfig.setName(defaultLlmProvider.getName());
            userLlmConfig.setUrl(defaultLlmProvider.getUrl());
            userLlmConfig.setConnected(false);
            userLlmConfigList.add(userLlmConfig);
        }
        userLlmConfigRepository.saveAll(userLlmConfigList);
    }

    /**
     * 为新用户初始化成就记录
     * @param userId 用户 id
     */
    private void initializeUserAchievements(Long userId) {
        List<AchievementDefinition> achievementDefinitions = achievementDefinitionRepository.findAll();
        List<UserAchievement> userAchievements = new ArrayList<>();
        for (AchievementDefinition ad : achievementDefinitions) {
            UserAchievement userAchievement = new UserAchievement();
            userAchievement.setUserId(userId);
            userAchievement.setAchievementCode(ad.getCode());
            userAchievement.setUnlocked(1);
            userAchievement.setProgress(0);
            userAchievements.add(userAchievement);
        }
        userAchievementRepository.saveAll(userAchievements);
    }

    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @return 用户信息
     */
    public UserInfo findByUsername(String username) {
        UserInfo userInfo = userInfoRepository.findByUsername(username);
        if (userInfo == null) {
            ExceptionUtils.throwDataNotFound("username not found");
        }
        return userInfo;
    }

    public UserInfo findById(Long id) {
        return userInfoRepository.findById(id).orElse(null);
    }

    /**
     * 更新用户信息
     * @param userInfo 用户信息
     */
    @Transactional
    public void updateInfo(Long userId, UserInfo userInfo) {
        UserInfo existing = findById(userId);
        if (existing == null) {
            ExceptionUtils.throwDataNotFound("user not found");
        }
        BeanUtils.copyProperties(userInfo, existing, ObjectTransformUtil.getNullPropertyNames(userInfo));
        userInfoRepository.save(existing);
    }

    // endregion

    // region 用户配置相关
    public List<UserConfig> findAllUserConfig() {
        return userConfigRepository.findAll();
    }

    /**
     * 获取用户配置信息
     * @param userId 用户ID
     * @return 用户配置信息
     */
    public UserConfig getUserConfig(Long userId) {
        return userConfigRepository.findByUserId(userId);
    }

//    public List<Integer> getLlmProviderList() {
//        return List.of(UserConstant.LLM_PROVIDER_OPENAI, UserConstant.LLM_PROVIDER_TAG);
//    }

    /**
     * 更新用户配置信息
     */
    @Transactional
    public void updateUserConfig(Long userId, BasicConfigUpdateRequest updateRequest) {
        // 校验用户是否存在
        UserConfig userConfigFromDb = getUserConfig(userId);
        if (userConfigFromDb == null) {
            ExceptionUtils.throwDataNotFound("user config not found");
        }
        Integer interval = updateRequest.getScanIntervalSeconds();
        if (interval != null) {
            if (interval < 3600 || interval > 43200) {
                ExceptionUtils.throwParamError("scan interval must be 3600-43200 seconds");
            }
        }
        BeanUtils.copyProperties(updateRequest, userConfigFromDb, ObjectTransformUtil.getNullPropertyNames(updateRequest));
        // 构建日报的cron表达式
        if (updateRequest.getDailyAnalysisTime() != null) {
            LocalTime time = updateRequest.getDailyAnalysisTime();
            userConfigFromDb.setDailyCron("0 " + time.getMinute() + " " + time.getHour() + " * * ?");
        }
        // 构建周报的cron表达式
        if (updateRequest.getWeeklyAnalysisTime() != null) {
            LocalTime time = updateRequest.getWeeklyAnalysisTime();
            userConfigFromDb.setWeeklyCron("0 " + time.getMinute() + " " + time.getHour() + " ? * " + updateRequest.getWeeklyAnalysisDay());
        }
        userConfigRepository.save(userConfigFromDb);
        // 刷新定时任务
        if (updateRequest.getScanIntervalSeconds() != null || updateRequest.getDailyAnalysisTime() != null || updateRequest.getWeeklyAnalysisTime() != null) {
            dynamicScheduledService.reloadUserTask(userId);
        }
    }

    public void uploadAvatar(Long userId, MultipartFile avatar) {
        // 校验文件是否为空
        if (avatar.isEmpty()) {
            throw new IllegalArgumentException("avatar file is empty");
        }
    }

    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        // 校验用户是否存在
        UserInfo userInfo = findById(userId);
        if (userInfo == null) {
            ExceptionUtils.throwDataNotFound("user not found");
        }
        // 校验旧密码是否正确（使用 BCrypt）
        if (!passwordEncoder.matches(request.getOldPassword(), userInfo.getPassword())) {
            ExceptionUtils.throwPasswordError();
        }
        // 密码加密（使用 BCrypt）
        String handledPassword = passwordEncoder.encode(request.getNewPassword());
        userInfo.setPassword(handledPassword);
        userInfoRepository.save(userInfo);
    }

    public void updateModelConfig(Long userId, List<UserLlmConfig> modelConfigList) {
        List<UserLlmConfig> llmProviderList = userLlmConfigRepository.findByUserId(userId);
        if (CollectionUtils.isEmpty(llmProviderList)) {
            ExceptionUtils.throwDataNotFound("user config not found");
        }
        for (UserLlmConfig userLlmConfig : llmProviderList) {
            for (UserLlmConfig llmConfig : modelConfigList) {
                if (Objects.equals(llmConfig.getId(), userLlmConfig.getId())) {
                    userLlmConfig.setUrl(llmConfig.getUrl());
                    userLlmConfig.setApiKey(llmConfig.getApiKey());
                    userLlmConfig.setConnected(llmConfig.isConnected());
                }
            }
        }
        userLlmConfigRepository.saveAll(llmProviderList);
    }

    public List<UserLlmConfig> getUserLlmProviderConfig(Long userId) {
        return userLlmConfigRepository.findByUserId(userId);
    }

    public void activeSelectedModel(SelectedModel selectedModel) {
        // Prevent duplicates
        List<SelectedModel> existing = selectedModelRepository.findByUserIdAndProviderId(selectedModel.getUserId(), selectedModel.getProviderId());
        boolean exists = existing.stream().anyMatch(m -> m.getModelName().equals(selectedModel.getModelName()));
        if (!exists) {
            selectedModelRepository.save(selectedModel);
        }
    }

    @Transactional
    public void deactiveSelectedModel(SelectedModel selectedModel) {
        selectedModelRepository.deleteByUserIdAndProviderIdAndModelName(
                selectedModel.getUserId(),
                selectedModel.getProviderId(),
                selectedModel.getModelName()
        );
    }

    public List<SelectedModel> getSelectedModel(Long userId, Integer providerId) {
        return selectedModelRepository.findByUserIdAndProviderId(userId, providerId);
    }

    /**
     * 获取用户的默认模型配置
     * @param userId 用户ID
     * @return 默认模型配置列表
     */
    public List<UserDefaultModelConfig> getUserDefaultModels(Long userId) {
        return userDefaultModelConfigRepository.findByUserId(userId);
    }

    /**
     * 更新用户的默认模型配置
     * @param userId 用户ID
     * @param modelConfigs 模型配置列表
     */
    @Transactional
    public void updateUserDefaultModels(Long userId, List<UserDefaultModelConfig> modelConfigs) {
        if (CollectionUtils.isEmpty(modelConfigs)) {
            return;
        }

        // 删除用户所有旧的默认模型配置
        userDefaultModelConfigRepository.deleteByUserId(userId);

        // 保存新的配置
        for (UserDefaultModelConfig config : modelConfigs) {
            config.setUserId(userId);
            config.setCreatedTime(new Date());
            config.setUpdatedTime(new Date());
        }
        userDefaultModelConfigRepository.saveAll(modelConfigs);
    }

    // endregion

    // region 个人中心统计相关

    /**
     * 获取用户统计数据
     * @param userId 用户ID
     * @return 统计数据
     */
    public UserStatsVo getUserStats(Long userId) {
        UserStatsVo stats = new UserStatsVo();

        // 1. 统计已同步文件数
        stats.setSyncFileCount(dataInfoRepository.countByUserId(userId));

        // 2. 统计已分析结果数
        stats.setAnalyzedCount(analysisResultRepository.countByUserId(userId));

        // 3. 统计合集数量
        stats.setCollectionCount(analysisCollectionRepository.countByUserId(userId));

        // 4. 统计标签数量 (主标签 + 子标签)
        long mainTagCount = mainTagRepository.countByUserId(userId);
        long subTagCount = subTagRepository.countByUserId(userId);
        stats.setTagCount(mainTagCount + subTagCount);

        // 5. 统计完成的测验数
        stats.setQuizCompletedCount(quizRecordRepository.countByUserIdAndStatus(userId, 1));

        // 6. 计算学习天数
        UserInfo userInfo = findById(userId);
        if (userInfo != null && userInfo.getCreateTime() != null) {
            long daysBetween = ChronoUnit.DAYS.between(
                    userInfo.getCreateTime().toInstant(),
                    Instant.now()
            );
            stats.setLearningDays(Math.max(1, daysBetween));
        }

        // 7. 获取最近活动 (最近10条)
        stats.setRecentActivities(getRecentActivities(userId));

        // 8. 检查并自动解锁成就
        checkAndUnlockAchievements(userId);

        // 9. 获取成就列表
        stats.setAchievements(getAchievementList(userId));

        // 10. 获取测验分数趋势
        stats.setQuizScoreTrend(getQuizScoreTrend(userId));

        // 11. 获取知识点掌握度
        stats.setKnowledgeMastery(getKnowledgeMastery(userId));

        // 12. 计算学习进度
        stats.setLearningProgress(calculateLearningProgress(userId));

        return stats;
    }

    /**
     * 获取用户成就列表
     * @param userId 用户ID
     * @return 成就列表
     */
    private List<UserStatsVo.AchievementVo> getAchievementList(Long userId) {
        List<UserStatsVo.AchievementVo> achievementList = new ArrayList<>();

        // 获取所有成就定义
        List<AchievementDefinition> definitions = achievementDefinitionRepository.findAllByOrderByOrderIndexAsc();

        // 获取用户成就记录
        Map<String, UserAchievement> userAchievementMap = new HashMap<>();
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserId(userId);
        for (UserAchievement ua : userAchievements) {
            userAchievementMap.put(ua.getAchievementCode(), ua);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // 组装成就列表
        for (AchievementDefinition definition : definitions) {
            UserStatsVo.AchievementVo vo = new UserStatsVo.AchievementVo();
            vo.setCode(definition.getCode());
            vo.setName(definition.getName());
            vo.setDescription(definition.getDescription());
            vo.setIcon(definition.getIcon());
            vo.setTarget(definition.getConditionValue());

            UserAchievement userAchievement = userAchievementMap.get(definition.getCode());
            if (userAchievement != null) {
                vo.setUnlocked(userAchievement.getUnlocked() == 1);
                vo.setProgress(userAchievement.getProgress());
                if (userAchievement.getUnlockedTime() != null) {
                    vo.setUnlockedTime(userAchievement.getUnlockedTime().format(formatter));
                }
            } else {
                vo.setUnlocked(false);
                vo.setProgress(0);
            }

            achievementList.add(vo);
        }

        return achievementList;
    }

    /**
     * 获取用户最近活动记录
     * @param userId 用户ID
     * @return 最近活动列表
     */
    private List<UserStatsVo.RecentActivityVo> getRecentActivities(Long userId) {
        List<UserStatsVo.RecentActivityVo> activities = new ArrayList<>();

        // 1. 最近同步记录 (取前3条)
        List<SyncRecord> syncRecords = syncRecordRepository.findTop3ByUserIdOrderByCreateTimeDesc(userId);
        for (SyncRecord record : syncRecords) {
            UserStatsVo.RecentActivityVo activity = new UserStatsVo.RecentActivityVo();
            activity.setType("sync");
            activity.setTime(formatTime(record.getCreateTime()));
            activity.setDetail("同步了" + record.getSyncCount() + "个文件，耗时" + record.getSpendTime() + "秒");
            activities.add(activity);
        }

        // 2. 最近创建的合集 (取前3条)
        List<AnalysisCollection> collections = analysisCollectionRepository.findTop3ByUserIdOrderByCreatedTimeDesc(userId);
        for (AnalysisCollection collection : collections) {
            UserStatsVo.RecentActivityVo activity = new UserStatsVo.RecentActivityVo();
            activity.setType("collection");
            activity.setTime(formatTime(collection.getCreatedTime()));
            activity.setDetail("创建了合集\"" + collection.getName() + "\"");
            activities.add(activity);
        }

        // 3. 最近完成的测验 (取前2条)
        List<QuizRecord> quizzes = quizRecordRepository.findTop2ByUserIdAndStatusOrderByCreatedTimeDesc(userId, 1);
        for (QuizRecord quiz : quizzes) {
            UserStatsVo.RecentActivityVo activity = new UserStatsVo.RecentActivityVo();
            activity.setType("quiz");
            activity.setTime(formatTime(quiz.getCreatedTime()));
            activity.setDetail("完成测验，总分" + quiz.getTotalScore());
            activities.add(activity);
        }

        // 4. 最近生成的报告 (取前2条)
        List<ReportData> reports = reportDataRepository.findTop2ByUserIdOrderByCreateTimeDesc(userId);
        for (ReportData report : reports) {
            UserStatsVo.RecentActivityVo activity = new UserStatsVo.RecentActivityVo();
            activity.setType("report");
            activity.setTime(formatTime(report.getCreateTime()));
            String reportType = report.getType() == 1 ? "日报" : "周报";
            activity.setDetail("生成" + reportType);
            activities.add(activity);
        }

        // 5. 按时间排序，只保留前10条
        activities.sort((a, b) -> b.getTime().compareTo(a.getTime()));
        return activities.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * 格式化时间
     * @param date 日期
     * @return 格式化后的时间字符串
     */
    private String formatTime(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(date);
    }

    /**
     * 格式化时间
     * @param date 日期
     * @return 格式化后的时间字符串
     */
    private String formatTime(LocalDateTime date) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return date.format(formatter);
    }

    // endregion

    // region 学习成就相关

    /**
     * 获取用户测验分数趋势
     * @param userId 用户ID
     * @return 测验分数趋势列表（按时间升序）
     */
    public List<UserStatsVo.QuizScoreTrendVo> getQuizScoreTrend(Long userId) {
        List<QuizRecord> quizRecords = quizRecordRepository.findAllByUserIdAndStatusOrderByCreatedTimeAsc(userId, 1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<UserStatsVo.QuizScoreTrendVo> trend = new ArrayList<>();
        for (QuizRecord record : quizRecords) {
            UserStatsVo.QuizScoreTrendVo vo = new UserStatsVo.QuizScoreTrendVo();
            vo.setDate(record.getCreatedTime().format(formatter));
            vo.setScore(record.getTotalScore());
            trend.add(vo);
        }
        return trend;
    }

    /**
     * 获取用户知识点掌握度
     * @param userId 用户ID
     * @return 知识点掌握度列表（按正确率降序）
     */
    public List<UserStatsVo.KnowledgeMasteryVo> getKnowledgeMastery(Long userId) {
        // 查询用户所有已完成测验的问题（包含标签信息）
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
        List<UserStatsVo.KnowledgeMasteryVo> masteryList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : tagCorrectCount.entrySet()) {
            String tagName = entry.getKey();
            int correctCount = entry.getValue();
            int totalCount = tagTotalCount.getOrDefault(tagName, 0);
            if (totalCount > 0) {
                UserStatsVo.KnowledgeMasteryVo vo = new UserStatsVo.KnowledgeMasteryVo();
                vo.setTagName(tagName);
                vo.setAccuracyRate(correctCount * 100.0 / totalCount);
                masteryList.add(vo);
            }
        }

        // 按正确率降序排序
        masteryList.sort((a, b) -> b.getAccuracyRate().compareTo(a.getAccuracyRate()));
        return masteryList;
    }

    /**
     * 检查并自动解锁用户成就
     * @param userId 用户ID
     * @return 新解锁的成就列表（成就代码列表）
     */
    @Transactional
    public List<String> checkAndUnlockAchievements(Long userId) {
        List<String> newlyUnlocked = new ArrayList<>();

        // 获取用户统计数据
        Long syncFileCount = dataInfoRepository.countByUserId(userId);
        Long analyzedCount = analysisResultRepository.countByUserId(userId);
        Long collectionCount = analysisCollectionRepository.countByUserId(userId);
        long unlockedAchievementCount = userAchievementRepository.countByUserIdAndUnlockedTrue(userId);
        long quizCompletedCount = quizRecordRepository.countByUserIdAndStatus(userId, 1);

        // 获取用户注册时间
        UserInfo userInfo = findById(userId);
        long learningDays = 1;
        if (userInfo != null && userInfo.getCreateTime() != null) {
            learningDays = Math.max(1, ChronoUnit.DAYS.between(
                    userInfo.getCreateTime().toInstant(),
                    Instant.now()
            ));
        }

        // 获取所有成就定义
        List<AchievementDefinition> definitions = achievementDefinitionRepository.findAllByOrderByOrderIndexAsc();

        for (AchievementDefinition definition : definitions) {
            String code = definition.getCode();
            String conditionType = definition.getConditionType();
            int conditionValue = definition.getConditionValue();

            // 获取用户当前的成就记录
            UserAchievement userAchievement = userAchievementRepository.findByUserIdAndAchievementCode(userId, code);

            if (userAchievement != null && userAchievement.getUnlocked() == 1) {
                // 已解锁，跳过
                continue;
            }

            // 根据条件类型判断是否满足解锁条件
            boolean shouldUnlock = false;
            int currentProgress = userAchievement != null ? userAchievement.getProgress() : 0;

            // TODO 需要优化
            switch (conditionType) {
                case "count_sync":
                    shouldUnlock = syncFileCount >= conditionValue;
                    currentProgress = syncFileCount.intValue();
                    break;
                case "count_analysis":
                    shouldUnlock = analyzedCount >= conditionValue;
                    currentProgress = analyzedCount.intValue();
                    break;
                case "count_collection":
                    shouldUnlock = collectionCount >= conditionValue;
                    currentProgress = collectionCount.intValue();
                    break;
                case "count_quiz":
                    shouldUnlock = quizCompletedCount >= conditionValue;
                    currentProgress = (int) quizCompletedCount;
                    break;
                case "continuous_days":
                    shouldUnlock = learningDays >= conditionValue;
                    currentProgress = (int) learningDays;
                    break;
                case "achievement":
                    shouldUnlock = unlockedAchievementCount >= conditionValue;
                    currentProgress = (int) unlockedAchievementCount;
                    break;
                case "perfect":
                    // 查询是否有满分的测验
                    List<QuizRecord> perfectQuizzes = quizRecordRepository.findAllByUserIdAndStatusOrderByCreatedTimeAsc(userId, 1);
                    shouldUnlock = perfectQuizzes.stream().anyMatch(q -> q.getTotalScore() == 100);
                    currentProgress = perfectQuizzes.stream().anyMatch(q -> q.getTotalScore() == 100) ? 100 : 0;
                    break;
                default:
                    // 未知条件类型，跳过
                    continue;
            }

            if (shouldUnlock && userAchievement != null) {
                // 解锁成就
                userAchievement.setUnlocked(1);
                userAchievement.setProgress(conditionValue);
                userAchievement.setUnlockedTime(LocalDateTime.now());
                userAchievementRepository.save(userAchievement);
                // 记录新解锁的成就
                newlyUnlocked.add(code);
            } else if (userAchievement != null) {
                // 更新进度
                userAchievement.setProgress(Math.min(currentProgress, conditionValue));
                userAchievementRepository.save(userAchievement);
            }
        }

        return newlyUnlocked;
    }

    /**
     * 计算用户学习进度
     * @param userId 用户ID
     * @return 学习进度数据
     */
    public UserStatsVo.LearningProgressVo calculateLearningProgress(Long userId) {
        UserStatsVo.LearningProgressVo progress = new UserStatsVo.LearningProgressVo();

        // 获取统计数据
        Long syncFileCount = dataInfoRepository.countByUserId(userId);
        Long analyzedCount = analysisResultRepository.countByUserId(userId);
        long quizCompletedCount = quizRecordRepository.countByUserIdAndStatus(userId, 1);
        long totalAchievements = achievementDefinitionRepository.count();
        long unlockedAchievements = userAchievementRepository.countByUserIdAndUnlockedTrue(userId);

        // 设置目标值（可根据实际需求调整）
        int targetSyncCount = 100;      // 目标同步文件数
        int targetAnalysisCount = 50;    // 目标分析结果数
        int targetQuizCount = 20;        // 目标测验完成数

        // 计算各维度进度
        int syncProgress = Math.min(100, (int) ((syncFileCount * 100.0) / targetSyncCount));
        int analysisProgress = Math.min(100, (int) ((analyzedCount * 100.0) / targetAnalysisCount));
        int quizProgress = Math.min(100, (int) ((quizCompletedCount * 100.0) / targetQuizCount));
        int achievementProgress = totalAchievements > 0
                ? (int) ((unlockedAchievements * 100.0) / totalAchievements)
                : 0;

        // 计算总体进度（各维度平均）
        int overallProgress = (syncProgress + analysisProgress + quizProgress + achievementProgress) / 4;

        progress.setOverallProgress(overallProgress);
        progress.setSyncProgress(syncProgress);
        progress.setAnalysisProgress(analysisProgress);
        progress.setQuizProgress(quizProgress);
        progress.setAchievementProgress(achievementProgress);

        return progress;
    }

    // endregion
}
