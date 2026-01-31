package com.review.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 * 用于管理异步任务执行，避免使用 new Thread() 创建线程
 *
 * @author Review Agent
 * @since 2026-01-31
 */
@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    /**
     * 分析任务线程池
     * 用于执行 AI 分析任务，这些任务通常比较耗时
     */
    @Bean("analysisTaskExecutor")
    public Executor analysisTaskExecutor() {
        log.info("初始化分析任务线程池...");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数（建议设置为 CPU 核心数）
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(corePoolSize);
        log.info("核心线程数: {}", corePoolSize);

        // 最大线程数（建议设置为 CPU 核心数 * 2）
        int maxPoolSize = corePoolSize * 2;
        executor.setMaxPoolSize(maxPoolSize);
        log.info("最大线程数: {}", maxPoolSize);

        // 队列容量（任务在队列中等待的最大数量）
        int queueCapacity = 100;
        executor.setQueueCapacity(queueCapacity);
        log.info("队列容量: {}", queueCapacity);

        // 线程名前缀（便于日志追踪）
        executor.setThreadNamePrefix("analysis-task-");

        // 拒绝策略（当队列满时的处理策略）
        // CallerRunsPolicy: 由调用线程执行该任务（降低请求速度）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 线程空闲时间（超过核心线程数的线程在空闲指定时间后会被销毁）
        executor.setKeepAliveSeconds(60);

        // 允许核心线程超时
        executor.setAllowCoreThreadTimeOut(true);

        // 等待所有任务完成后再关闭线程池（优雅关闭）
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待任务完成的最大时间（秒）
        executor.setAwaitTerminationSeconds(60);

        // 初始化线程池
        executor.initialize();

        log.info("分析任务线程池初始化完成");
        return executor;
    }

    /**
     * SSE 连接管理线程池
     * 用于管理 SSE（Server-Sent Events）连接的心跳检测
     */
    @Bean("sseTaskExecutor")
    public Executor sseTaskExecutor() {
        log.info("初始化 SSE 任务线程池...");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // SSE 心跳任务比较轻量，不需要太多线程
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("sse-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setKeepAliveSeconds(30);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("SSE 任务线程池初始化完成");
        return executor;
    }

    /**
     * 默认异步任务线程池
     * 用于执行一般的异步任务
     */
    @Bean("defaultAsyncExecutor")
    public Executor defaultAsyncExecutor() {
        log.info("初始化默认异步任务线程池...");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("默认异步任务线程池初始化完成");
        return executor;
    }
}
