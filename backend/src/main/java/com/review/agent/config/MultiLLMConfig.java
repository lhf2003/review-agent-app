package com.review.agent.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class MultiLLMConfig {
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    @Value("${spring.ai.dashscope.embedding.options.model}")
    private String embeddingModel;
    @Value("${spring.ai.dashscope.embedding.options.dimensions}")
    private Integer dimensions;

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

    @Bean
    public DashScopeApi dashScopeApi(@Qualifier("restClient") RestClient.Builder restClient) {
        return DashScopeApi.builder().apiKey(apiKey).restClientBuilder(restClient).build();
    }


    // region 向量嵌入模型配置
    @Primary
    @Bean
    public EmbeddingModel dashScopeEmbeddingModel(DashScopeApi dashScopeApi) {
        DashScopeEmbeddingOptions dashScopeEmbeddingOptions = DashScopeEmbeddingOptions.builder()
                .model(embeddingModel)
                .dimensions(dimensions)
                .build();
        return new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED, dashScopeEmbeddingOptions);
    }

//    @Bean
//    public EmbeddingModel ollamaEmbeddingModel(OllamaApi ollamaApi) {
//        return OllamaEmbeddingModel.builder()
//                .ollamaApi(ollamaApi)
//                .build();
//    }

    // endregion

    // region 模型配置 - 不同场景下的模型选择

    /**
     * 闲聊助手专用模型 - 擅长基础对话和问题回答
     */
    @Bean("chatModel")
    public DashScopeChatModel chatModel(DashScopeApi dashScopeApi) {
        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(CHAT_MODEL)
                        .temperature(CHAT_TEMPERATURE)
                        .maxToken(CHAT_MAX_TOKENS)
                        .enableThinking(false)
                        .enableSearch(false)
                        .stream(true)
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
                        .model(ANALYSIS_MODEL)
                        .temperature(ANALYSIS_TEMPERATURE)
                        .maxToken(ANALYSIS_MAX_TOKENS)
                        .enableThinking(false)
                        .enableSearch(false)
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
                        .model(CLASSIFY_MODEL)
                        .temperature(CLASSIFY_TEMPERATURE)
                        .maxToken(CLASSIFY_MAX_TOKENS)
                        .enableThinking(false)
                        .enableSearch(false)
                        .build())
                .build();
    }

    /**
     * 提取助手专用模型
     */
    @Bean("extractChatModel")
    public DashScopeChatModel extractChatModel(DashScopeApi dashScopeApi) {
        DashScopeApi api = DashScopeApi.builder()
                .baseUrl("https://dashscope.aliyuncs.com")
                .completionsPath("/api/v1/services/aigc/multimodal-generation/generation")
                .apiKey(apiKey)
                .build();
        return DashScopeChatModel.builder()
                .dashScopeApi(api)
                .defaultOptions(DashScopeChatOptions.builder()
                        .model(EXTRACT_MODEL)
                        .temperature(EXTRACT_TEMPERATURE)
                        .maxToken(EXTRACT_MAX_TOKENS)
                        .incrementalOutput(true)
                        .build())
                .build();
    }


    /**
     * 本地模型
     * @param ollamaChatModel 本地模型
     * @return 本地模型的 ChatClient
     */
    @Bean
    public ChatClient ollamachatClient(@Qualifier("ollamaChatModel") OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel).defaultAdvisors(new TokenLoggerAdvisor()).build();
    }
    // endregion

    // region ChatClient 配置 - 为不同模型创建对应的ChatClient

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

    /**
     * 思维范式识别专用ChatClient
     * 复用分类模型配置（低温度，精确输出）
     */
    @Bean("paradigmChatClient")
    public ChatClient paradigmChatClient(@Qualifier("classifyChatModel") DashScopeChatModel classifyChatModel) {
        return ChatClient.builder(classifyChatModel).defaultAdvisors(new TokenLoggerAdvisor()).build();
    }
    // endregion
}
