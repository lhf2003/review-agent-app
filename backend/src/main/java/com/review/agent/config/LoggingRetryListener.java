package com.review.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Component
public class LoggingRetryListener implements RetryListener {

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // 全部尝试结束（成功或失败）
        log.info("结束重试，状态: {}，异常: {}",
                context.isExhaustedOnly() ? "已耗尽重试" : "成功",
                throwable != null ? throwable.getClass().getSimpleName() : "无");
        throw new ResourceAccessException("重试失败");
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        // 每次重试失败后调用
        log.warn("第 {} 次重试失败，异常类型: {}，消息: {}",
                context.getRetryCount(),
                throwable.getClass().getSimpleName(),
                throwable.getMessage());

    }
}
