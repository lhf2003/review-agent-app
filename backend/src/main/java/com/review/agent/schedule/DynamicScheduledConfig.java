package com.review.agent.schedule;

import com.review.agent.entity.UserConfig;
import com.review.agent.service.DataInfoService;
import com.review.agent.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 动态定时任务配置类
 */
@Slf4j
@Configuration
public class DynamicScheduledConfig implements SchedulingConfigurer {
    @Resource
    private UserService userService;
    @Resource
    private DataInfoService dataInfoService;

    /**
     * 存储用户任务的映射，key 为用户ID，value 为 ScheduledFuture
     */
    private final Map<Long, ScheduledFuture<?>> scheduledTaskMap = new ConcurrentHashMap<>();

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        // 使用线程池化的 TaskScheduler
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("dynamic-scheduled-task-");
        scheduler.initialize();
        registrar.setTaskScheduler(scheduler);

        // 初始加载所有用户的任务
        reloadAllTasks(scheduler);
    }

    // 重新加载所有任务（可用于初始化或刷新）
    public void reloadAllTasks(TaskScheduler scheduler) {
        // 清除旧任务
        for (ScheduledFuture<?> future : scheduledTaskMap.values()) {
            future.cancel(true);
        }
        scheduledTaskMap.clear();

        // 加载用户任务配置（假设从 DB 获取）
        List<UserConfig> userConfigList = userService.findAllUserConfig();

        for (UserConfig config : userConfigList) {
            scheduleTask(config, scheduler);
        }
    }

    /**
     * 执行指定用户任务
     * @param config 用户配置信息
     * @param scheduler 任务调度器
     */
    public void scheduleTask(UserConfig config, TaskScheduler scheduler) {
        Runnable task = () -> {
            log.info("开始执行用户: " + config.getUserId());
            try {
                // 调用同步数据逻辑
                dataInfoService.syncData(config.getUserId());
            } catch (IOException e) {
                log.error("定时同步数据失败，用户ID: {}", config.getUserId(), e);
            }
        };

        // 取消已有任务
        ScheduledFuture<?> existingFuture = scheduledTaskMap.get(config.getUserId());
        if (existingFuture != null) {
            existingFuture.cancel(true);
        }

        // 按 fixedDelay 配置周期
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(task, config.getScanIntervalSeconds() * 1000L);
        scheduledTaskMap.put(config.getUserId(), future);
    }
    
    // 删除某个用户任务
    public void removeTask(Long userId) {
        ScheduledFuture<?> future = scheduledTaskMap.get(userId);
        if (future != null) {
            future.cancel(true);
            scheduledTaskMap.remove(userId);
        }
    }
}