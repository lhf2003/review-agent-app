//package com.review.agent.common.config;
//
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.ollama.OllamaEmbeddingModel;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.ai.vectorstore.redis.RedisVectorStore;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import redis.clients.jedis.JedisPooled;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Configuration
//public class VectorStoreConfig {
//
//    @Value("${spring.data.redis.host:localhost}")
//    private String host;
//
//    @Value("${spring.data.redis.port:6379}")
//    private int port;
//
//
//    @Bean
//    public JedisPooled jedisPooled() {
//        return new JedisPooled(host, port);
//    }
//
//    @Bean
//    public VectorStore vectorStore(JedisPooled jedisPooled, EmbeddingModel embeddingModel) {
//        List<RedisVectorStore.MetadataField> metadataFieldList = new ArrayList<>();
//        metadataFieldList.add(RedisVectorStore.MetadataField.tag("userId"));
//        metadataFieldList.add(RedisVectorStore.MetadataField.text("response"));
//
//        return RedisVectorStore.builder(jedisPooled, embeddingModel)
////                .indexName(indexName)                // Optional: defaults to "spring-ai-index"
////                .prefix(prefix)                  // Optional: defaults to "embedding:"
//                .metadataFields(metadataFieldList)
//                .initializeSchema(true)                   // Optional: defaults to false
////                .batchingStrategy(new TokenCountBatchingStrategy()) // Optional: defaults to TokenCountBatchingStrategy
//                .build();
//    }
//}