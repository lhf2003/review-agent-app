package com.review.agent.strategy;

import com.review.agent.common.enums.LlmProvider;
import com.review.agent.entity.pojo.UserLlmConfig;
import com.review.agent.repository.UserLlmConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LlmClientFactory {

    private final UserLlmConfigRepository userLlmConfigRepository;
    private final List<LlmProviderStrategy> strategies;

    /**
     * 获取当前上下文适用的 AI 客户端
     */
    public ChatClient getClient(Long userId) {
        // 1. 查询用户个性化配置
        List<UserLlmConfig> providerList = userLlmConfigRepository.findByUserId(userId);

        // 2. 找到对应的策略构建器
//        return strategies.stream()
//                .filter(s -> s.supports(config.getProvider()))
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("不支持的供应商: " + config.getProvider()))
//                .buildClient(config.getApiKey(), config.getModelName(), config.getBaseUrl());
        return null;
    }
}