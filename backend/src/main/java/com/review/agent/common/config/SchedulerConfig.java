package com.review.agent.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("DynamicScheduling-");
        scheduler.setErrorHandler(throwable -> log.error("Unexpected error during scheduling: " + throwable.getMessage(), throwable));
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }
}
