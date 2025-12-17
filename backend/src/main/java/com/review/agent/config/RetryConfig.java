package com.review.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class RetryConfig {
    
    @Bean
    public LoggingRetryListener loggingRetryListener() {
        return new LoggingRetryListener();
    }
}