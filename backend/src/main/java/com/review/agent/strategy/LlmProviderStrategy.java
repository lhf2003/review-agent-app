package com.review.agent.strategy;


import com.review.agent.entity.pojo.UserLlmConfig;
import org.springframework.ai.chat.client.ChatClient;

public interface LlmProviderStrategy {
    /**
     * 判断当前策略是否支持该提供商
     */
    boolean supports(String provider);

    boolean connectLlmProvider(UserLlmConfig llmProvider);

    /**
     * 动态构建客户端
     * @param apiKey 解密后的Key
     * @param modelName 模型名称
     * @param baseUrl 接口地址
     * @return LangChain4j 的通用接口
     */
    ChatClient buildClient(String apiKey, String modelName, String baseUrl);
}