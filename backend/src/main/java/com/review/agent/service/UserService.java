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
import org.springframework.boot.autoconfigure.web.format.DateTimeFormatters;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.review.agent.common.constant.UserConstant.SALT;

@Service
public class UserService {
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
        // 密码加密
        String handledPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
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
        // 校验旧密码是否正确
        if (!userInfo.getPassword().equals(DigestUtils.md5DigestAsHex((SALT + request.getOldPassword()).getBytes()))) {
            ExceptionUtils.throwPasswordError();
        }
        // 密码加密
        String handledPassword = DigestUtils.md5DigestAsHex((SALT + request.getNewPassword()).getBytes());
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

        return stats;
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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return formatter.format(date);
    }

    // endregion
}
