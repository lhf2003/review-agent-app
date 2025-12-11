package com.review.agent.common.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiLLMConfig {

    // 模型配置常量 - 默认使用百炼模型
    private static final String CHAT_MODEL = "qwen-plus";
    private static final String ANALYSIS_MODEL = "qwen-plus";
    private static final String CLASSIFY_MODEL = "qwen-plus";
    private static final String EXTRACT_MODEL = "qwen-plus";

    // 温度参数常量 - 控制模型输出的随机性
    private static final double CHAT_TEMPERATURE = 0.6;
    private static final double ANALYSIS_TEMPERATURE = 0.8;
    private static final double CLASSIFY_TEMPERATURE = 0.1;
    public static final double EXTRACT_TEMPERATURE = 0.6;

    // Token限制常量 - 控制模型输出的最大token数
    private static final int CHAT_MAX_TOKENS = 30000;
    private static final int ANALYSIS_MAX_TOKENS = 30000;
    private static final int CLASSIFY_MAX_TOKENS = 1000;
    private static final int EXTRACT_MAX_TOKENS = 1000;


    @Bean("chatModel")
    public DashScopeChatModel chatModel(DashScopeApi dashScopeApi) {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(CHAT_MODEL)
                        .withTemperature(CHAT_TEMPERATURE)
                        .withMaxToken(CHAT_MAX_TOKENS)
                        .withEnableThinking(false)
                        .withEnableSearch(false)
                        .withStream(true)
                        .build())
                .build();
    }

    /**
     * 数据分析助手专用模型 - 擅长数据分析
     */
    @Bean("analysisChatModel")
    public DashScopeChatModel analysisChatModel(DashScopeApi dashScopeApi) {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(ANALYSIS_MODEL)
                        .withTemperature(ANALYSIS_TEMPERATURE)
                        .withMaxToken(ANALYSIS_MAX_TOKENS)
                        .withEnableThinking(false)
                        .withEnableSearch(false)
                        .build())
                .build();
    }

    /**
     * 文案助手专用模型 - 擅长文案创作和编辑
     */
    @Bean("classifyChatModel")
    public DashScopeChatModel classifyChatModel(DashScopeApi dashScopeApi) {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(CLASSIFY_MODEL)
                        .withTemperature(CLASSIFY_TEMPERATURE)
                        .withMaxToken(CLASSIFY_MAX_TOKENS)
                        .withEnableThinking(false)
                        .withEnableSearch(false)
                        .build())
                .build();
    }

    /**
     * 提取助手专用模型
     */
    @Bean("extractChatModel")
    public DashScopeChatModel extractChatModel(DashScopeApi dashScopeApi) {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel(EXTRACT_MODEL)
                        .withTemperature(EXTRACT_TEMPERATURE)
                        .withMaxToken(EXTRACT_MAX_TOKENS)
                        .build())
                .build();
    }


    /**
     * 本地模型
     * @param ollamaChatModel 本地模型
     * @return 本地模型的ChatClient
     */
    @Bean
    public ChatClient ollamachatClient(@Qualifier("ollamaChatModel") OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).defaultAdvisors(new TokenLoggerAdvisor()).build();
    }

    /**
     * 数据分析助手专用ChatClient
     */
    @Bean("analysisChatClient")
    public ChatClient analysisChatClient(@Qualifier("analysisChatModel") DashScopeChatModel analysisChatModel) {
        return ChatClient.builder(analysisChatModel).defaultAdvisors(new TokenLoggerAdvisor()).build();
    }

    /**
     * 分类助手专用ChatClient
     */
    @Bean("classifyChatClient")
    public ChatClient classifyChatClient(@Qualifier("classifyChatModel") DashScopeChatModel classifyChatModel) {
        return ChatClient.builder(classifyChatModel).defaultAdvisors(new TokenLoggerAdvisor()).build();
    }

    /**
     * 提取助手专用ChatClient
     */
    @Bean("extractChatClient")
    public ChatClient imageChatClient(@Qualifier("extractChatModel") DashScopeChatModel imageChatModel) {
        return ChatClient.builder(imageChatModel).defaultAdvisors(new TokenLoggerAdvisor()).build();
    }
}
