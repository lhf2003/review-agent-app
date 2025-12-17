package com.review.agent.schedule;

import com.review.agent.entity.pojo.UserConfig;
import com.review.agent.service.DataInfoService;
import com.review.agent.service.ReportService;
import com.review.agent.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态定时任务配置类
 */
@Slf4j
@Service
public class DynamicScheduledService {
    @Resource
    private UserService userService;
    @Resource
    private DataInfoService dataInfoService;
    @Resource
    private ReportService reportService;
    @Resource
    private TaskScheduler taskScheduler;

    // TODO 后面改为redis存储
    /**
     * 存储用户同步数据任务的映射，key 为用户ID，value 为 ScheduledFuture
     */
    private final Map<Long, ScheduledFuture<?>> scheduledDataMap = new ConcurrentHashMap<>();
    /**
     * 存储用户生成报告任务的映射，key 为用户ID，value 为 ScheduledFuture
     */
    private final Map<Long, ScheduledFuture<?>> scheduledReportDailyMap = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> scheduledReportWeeklyMap = new ConcurrentHashMap<>();


//    @PostConstruct
    public void init() {
        reloadAllTasks(taskScheduler);
    }

    /**
     * 重新加载指定用户的任务
     * @param userId 用户ID
     */
    public void reloadUserTask(Long userId) {
        UserConfig config = userService.getUserConfig(userId);
        if (config != null) {
            scheduleTask(config, taskScheduler);
            scheduleReportDailyTask(config, taskScheduler);
            scheduleReportWeeklyTask(config, taskScheduler);
        }
    }

    // 重新加载所有任务（可用于初始化或刷新）
    public void reloadAllTasks(TaskScheduler scheduler) {
        // 清除旧任务
        for (ScheduledFuture<?> future : scheduledDataMap.values()) {
            future.cancel(true);
        }
        scheduledDataMap.clear();

        // 加载用户任务配置
        List<UserConfig> userConfigList = userService.findAllUserConfig();

        for (UserConfig config : userConfigList) {
            scheduleTask(config, scheduler);
            scheduleReportDailyTask(config, scheduler);
            scheduleReportWeeklyTask(config, scheduler);
        }
    }

    /**
     * 执行指定用户的同步数据任务
     * @param config 用户配置信息
     * @param scheduler 任务调度器
     */
    public void scheduleTask(UserConfig config, TaskScheduler scheduler) {
        // 取消已有任务
        ScheduledFuture<?> existingFuture = scheduledDataMap.get(config.getUserId());
        if (existingFuture != null) {
            existingFuture.cancel(true);
            scheduledDataMap.remove(config.getUserId());
        }

        if (Boolean.FALSE.equals(config.getAutoScanEnabled())) {
            return;
        }

        Runnable task = () -> {
            log.info("开始执行用户: " + config.getUserId());
            try {
                // 调用同步数据逻辑
                dataInfoService.syncData(config.getUserId());
            } catch (IOException e) {
                log.error("定时同步数据失败，用户ID: {}", config.getUserId(), e);
            }
        };

        // 按 fixedDelay 配置周期
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(task, Duration.ofSeconds(config.getScanIntervalSeconds()));
        scheduledDataMap.put(config.getUserId(), future);
    }

    /**
     * 执行指定用户的日报任务
     * @param config 用户配置信息
     * @param scheduler 任务调度器
     */
    public void scheduleReportDailyTask(UserConfig config, TaskScheduler scheduler) {
        // 取消已有任务
        ScheduledFuture<?> existingFuture = scheduledReportDailyMap.get(config.getUserId());
        if (existingFuture != null) {
            existingFuture.cancel(true);
            scheduledReportDailyMap.remove(config.getUserId());
        }

        if (!Boolean.TRUE.equals(config.getDailyEnabled())) {
            return;
        }
        Runnable task = () -> {
            log.info("开始执行用户: {}, 生成日报任务", config.getUserId());
            // 调用生成日报逻辑
            reportService.generateDailyReport(config.getUserId());
        };

        // 按 fixedDelay 配置周期
        ScheduledFuture<?> future = scheduler.schedule(task, new CronTrigger(config.getDailyCron()));
        scheduledReportDailyMap.put(config.getUserId(), future);
    }

    /**
     * 执行指定用户的周报任务
     * @param config 用户配置信息
     * @param scheduler 任务调度器
     */
    public void scheduleReportWeeklyTask(UserConfig config, TaskScheduler scheduler) {
        // 取消已有任务
        ScheduledFuture<?> existingFuture = scheduledReportWeeklyMap.get(config.getUserId());
        if (existingFuture != null) {
            existingFuture.cancel(true);
            scheduledReportWeeklyMap.remove(config.getUserId());
        }

        if (!Boolean.TRUE.equals(config.getWeeklyEnabled())) {
            return;
        }
        Runnable task = () -> {
            log.info("开始执行用户: {}, 生成周报任务", config.getUserId());
            // 调用生成周报逻辑
            reportService.generateWeeklyReport(config.getUserId());
        };

        // 按 fixedDelay 配置周期
        ScheduledFuture<?> future = scheduler.schedule(task, new CronTrigger(config.getWeeklyCron()));
        scheduledReportWeeklyMap.put(config.getUserId(), future);
    }

}