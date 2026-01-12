package com.review.agent.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class VectorStoreConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(host, port);
    }

    @Bean(name = "chatVectorStore")
    public RedisVectorStore chatVectorStore(JedisPooled jedisPooled, EmbeddingModel dashscopeEmbeddingModel) {
        System.out.println(dashscopeEmbeddingModel.dimensions());
        List<RedisVectorStore.MetadataField> metadataFieldList = new ArrayList<>();
        metadataFieldList.add(RedisVectorStore.MetadataField.tag("userId"));
        metadataFieldList.add(RedisVectorStore.MetadataField.text("response"));

        return RedisVectorStore.builder(jedisPooled, dashscopeEmbeddingModel)
//                .indexName(indexName)                // Optional: defaults to "spring-ai-index"
//                .prefix(prefix)                  // Optional: defaults to "embedding:"
                .metadataFields(metadataFieldList)
                .initializeSchema(true)                   // Optional: defaults to false
//                .batchingStrategy(new TokenCountBatchingStrategy()) // Optional: defaults to TokenCountBatchingStrategy
                .build();
    }
    @Bean(name = "analysisResultVectorStore")
    public RedisVectorStore analysisResultVectorStore(JedisPooled jedisPooled, EmbeddingModel ollamaEmbeddingModel) {
        System.out.println(ollamaEmbeddingModel.dimensions());
        List<RedisVectorStore.MetadataField> metadataFieldList = new ArrayList<>();
        metadataFieldList.add(RedisVectorStore.MetadataField.tag("userId"));
        metadataFieldList.add(RedisVectorStore.MetadataField.text("solution"));
        metadataFieldList.add(RedisVectorStore.MetadataField.text("sessionContent"));
        metadataFieldList.add(RedisVectorStore.MetadataField.text("fileId"));

        return RedisVectorStore.builder(jedisPooled, ollamaEmbeddingModel)
                .indexName("analysis-result-index")                // Optional: defaults to "analysis-result-index"
                .prefix("embedding:")                  // Optional: defaults to "embedding:"
                .metadataFields(metadataFieldList)
                .initializeSchema(true)                   // Optional: defaults to false
//                .batchingStrategy(new TokenCountBatchingStrategy()) // Optional: defaults to TokenCountBatchingStrategy
                .build();
    }
}