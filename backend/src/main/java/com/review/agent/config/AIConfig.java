package com.review.agent.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Spring AI Alibaba配置 配置DashScope模型
 */
@Configuration
public class AIConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

//    @Value("${spring.ai.dashscope.chat.model}")
//    private String chatModel;
//
//    @Value("${spring.ai.dashscope.embedding.model}")
//    private String embeddingModel;
//
//    @Value("${spring.ai.dashscope.embedding.options.dimensions}")
//    private Integer dimensions;



    @Bean
    public DashScopeApi dashScopeApi(@Qualifier("restClient") RestClient.Builder restClient) {
        return DashScopeApi.builder().apiKey(apiKey).restClientBuilder(restClient).build();
    }

//    @Bean
//    public DashScopeChatModel dashScopeChatModel(DashScopeApi dashScopeApi) {
//        return DashScopeChatModel.builder()
//                .dashScopeApi(dashScopeApi)
//                .defaultOptions(DashScopeChatOptions.builder().model(chatModel).build())
//                .build();
//    }


    @Bean
    public EmbeddingModel embeddingModel(DashScopeApi dashScopeApi) {
        return new DashScopeEmbeddingModel(dashScopeApi);
    }
}
