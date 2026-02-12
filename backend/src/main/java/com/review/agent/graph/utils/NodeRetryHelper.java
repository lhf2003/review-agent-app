package com.review.agent.graph.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.function.Supplier;

/**
 * 节点重试辅助类
 * 提供统一的重试机制和降级策略
 */
@Slf4j
public final class NodeRetryHelper {

    private NodeRetryHelper() {
        // 工具类不允许实例化
    }

    /**
     * 默认重试次数
     */
    private static final int DEFAULT_MAX_RETRIES = 3;

    /**
     * 重试间隔（毫秒）
     */
    private static final long RETRY_DELAY_MS = 1000;

    /**
     * 带重试机制的 LLM 调用
     *
     * @param <T> 返回类型
     * @param operation 操作名称（用于日志）
     * @param supplier LLM 调用逻辑
     * @param fallback 降级逻辑（可为 null）
     * @return LLM 响应结果，失败时返回降级结果或 null
     */
    public static <T> T executeWithRetry(String operation, Supplier<T> supplier, Supplier<T> fallback) {
        return executeWithRetry(operation, supplier, fallback, DEFAULT_MAX_RETRIES);
    }

    /**
     * 带重试机制的 LLM 调用（指定重试次数）
     *
     * @param <T> 返回类型
     * @param operation 操作名称（用于日志）
     * @param supplier LLM 调用逻辑
     * @param fallback 降级逻辑（可为 null）
     * @param maxRetries 最大重试次数
     * @return LLM 响应结果，失败时返回降级结果或 null
     */
    public static <T> T executeWithRetry(String operation, Supplier<T> supplier, Supplier<T> fallback, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                T result = supplier.get();
                if (result != null) {
                    if (attempt > 0) {
                        log.info("[{}] 在第 {} 次尝试成功", operation, attempt + 1);
                    }
                    return result;
                }
                log.warn("[{}] 第 {} 次调用返回 null", operation, attempt + 1);
            } catch (Exception e) {
                lastException = e;
                log.error("[{}] 第 {} 次调用失败: {}", operation, attempt + 1, e.getMessage());
            }

            attempt++;

            // 如果还有重试机会，等待后重试
            if (attempt < maxRetries) {
                try {
                    long delay = RETRY_DELAY_MS * attempt; // 递增延迟
                    log.info("[{}] 等待 {}ms 后进行第 {} 次重试...", operation, delay, attempt + 1);
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("[{}] 重试等待被中断", operation);
                    break;
                }
            }
        }

        // 所有重试都失败，尝试降级
        log.error("[{}] 所有 {} 次尝试均失败，执行降级策略", operation, maxRetries);
        if (lastException != null) {
            log.error("[{}] 最后一次异常详情", operation, lastException);
        }

        if (fallback != null) {
            try {
                T fallbackResult = fallback.get();
                log.info("[{}] 降级策略执行成功", operation);
                return fallbackResult;
            } catch (Exception e) {
                log.error("[{}] 降级策略执行失败: {}", operation, e.getMessage());
            }
        }

        return null;
    }

    /**
     * 带重试机制的 LLM 调用（无降级）
     *
     * @param <T> 返回类型
     * @param operation 操作名称（用于日志）
     * @param supplier LLM 调用逻辑
     * @return LLM 响应结果，失败时返回 null
     */
    public static <T> T executeWithRetry(String operation, Supplier<T> supplier) {
        return executeWithRetry(operation, supplier, null, DEFAULT_MAX_RETRIES);
    }

    /**
     * 带 LLM 调用规范的请求构建器
     * 用于记录请求详情和错误信息
     */
    public static class LlmRequestBuilder {
        private String operation;
        private String systemPrompt;
        private String userPrompt;
        private ChatClient chatClient;

        public LlmRequestBuilder operation(String operation) {
            this.operation = operation;
            return this;
        }

        public LlmRequestBuilder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public LlmRequestBuilder userPrompt(String userPrompt) {
            this.userPrompt = userPrompt;
            return this;
        }

        public LlmRequestBuilder chatClient(ChatClient chatClient) {
            this.chatClient = chatClient;
            return this;
        }

        /**
         * 执行调用并返回实体
         */
        public <T> T execute(Class<T> responseType) {
            return execute(responseType, null);
        }

        /**
         * 执行调用并返回实体（带降级）
         */
        public <T> T execute(Class<T> responseType, Supplier<T> fallback) {
            if (chatClient == null) {
                log.error("[{}] ChatClient 未配置", operation);
                return fallback != null ? fallback.get() : null;
            }

            log.debug("[{}] 开始 LLM 调用, userPrompt 长度: {}",
                operation,
                userPrompt != null ? userPrompt.length() : 0
            );

            return executeWithRetry(
                operation,
                () -> {
                    var promptSpec = chatClient.prompt();
                    if (systemPrompt != null && !systemPrompt.isEmpty()) {
                        promptSpec = promptSpec.system(systemPrompt);
                    }
                    return promptSpec.user(userPrompt).call().entity(responseType);
                },
                fallback
            );
        }

        /**
         * 执行调用并返回字符串内容
         */
        public String executeContent() {
            return executeContent(null);
        }

        /**
         * 执行调用并返回字符串内容（带降级）
         */
        public String executeContent(Supplier<String> fallback) {
            if (chatClient == null) {
                log.error("[{}] ChatClient 未配置", operation);
                return fallback != null ? fallback.get() : null;
            }

            log.debug("[{}] 开始 LLM 调用, userPrompt 长度: {}",
                operation,
                userPrompt != null ? userPrompt.length() : 0
            );

            return executeWithRetry(
                operation,
                () -> {
                    var promptSpec = chatClient.prompt();
                    if (systemPrompt != null && !systemPrompt.isEmpty()) {
                        promptSpec = promptSpec.system(systemPrompt);
                    }
                    return promptSpec.user(userPrompt).call().content();
                },
                fallback
            );
        }
    }

    /**
     * 创建 LLM 请求构建器
     */
    public static LlmRequestBuilder builder() {
        return new LlmRequestBuilder();
    }
}
