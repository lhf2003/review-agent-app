package com.review.agent.strategy;

import com.review.agent.entity.pojo.UserLlmConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.stereotype.Component;

import static org.springframework.ai.model.SpringAIModels.DEEPSEEK;

@Component
public class DeepSeekStrategy implements LlmProviderStrategy {

    @Override
    public boolean supports(String provider) {
        return provider.equals(DEEPSEEK);
    }

    @Override
    public boolean connectLlmProvider(UserLlmConfig llmProvider) {
        return false;
    }

    @Override
    public ChatClient buildClient(String apiKey, String modelName, String baseUrl) {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey(apiKey)
                .build();
        DeepSeekChatOptions options = DeepSeekChatOptions.builder()
                .model(modelName)
                .build();
        DeepSeekChatModel deepSeekChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .defaultOptions(options)
                .build();
        return ChatClient.builder(deepSeekChatModel)
                .defaultOptions(options)
                .build();
    }
}