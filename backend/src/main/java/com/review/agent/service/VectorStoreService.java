package com.review.agent.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.util.List;


@Service
public class VectorStoreService {
    private final RedisVectorStore chatVectorStore;
    private final RedisVectorStore analysisResultVectorStore;
    private final JedisPooled jedisPooled;

    public VectorStoreService(@Qualifier("chatVectorStore") RedisVectorStore chatVectorStore, @Qualifier("analysisResultVectorStore") RedisVectorStore analysisResultVectorStore, JedisPooled jedisPooled) {
        this.chatVectorStore = chatVectorStore;
        this.analysisResultVectorStore = analysisResultVectorStore;
        this.jedisPooled = jedisPooled;

    }

    /**
     * 搜索一个相似文档
     * @param request 用户文本
     * @return 相似文档
     */
    public Document searchOne(String request) {
        List<Document> documentList = chatVectorStore.similaritySearch(SearchRequest.builder()
                .query(request)
                .similarityThreshold(0.9)
                .topK(1)
                .build());
        if (documentList.isEmpty()) {
            return null;
        }
        return documentList.get(0);
    }

    /**
     * 搜索一个相似文档
     * @param request 用户文本
     * @return 相似文档
     */
    public Document searchOneAnalysisResult(String request) {
        List<Document> documentList = analysisResultVectorStore.similaritySearch(SearchRequest.builder()
                .query(request)
                .similarityThreshold(0.9)
                .topK(1)
                .build());
        if (documentList.isEmpty()) {
            return null;
        }
        return documentList.get(0);
    }

    /**
     * 添加一个文档到向量数据库
     * @param document 文档
     */
    public void addOne(Document document) {
        chatVectorStore.add(List.of(document));
    }

    /**
     * 添加一个文档到分析结果向量数据库
     * @param document 文档
     */
    public void addOneAnalysisResult(Document document) {
        analysisResultVectorStore.add(List.of(document));
    }

    /**
     * 搜索当前用户相似问题的文档
     * @param problemStatement 问题语句
     * @param userId 用户ID
     * @return 相似文档列表
     */
    public List<Document> searchSimilarity(String problemStatement, Long userId) {
        return chatVectorStore.similaritySearch(SearchRequest.builder()
                .query(problemStatement)
                .similarityThreshold(0.8)
                .filterExpression(new FilterExpressionBuilder().eq("userId", userId).build())
                .build());
    }

    /**
     * 搜索当前用户相似问题的分析结果文档
     * @param problemStatement 问题语句
     * @param userId 用户ID
     * @return 相似文档列表
     */
    public List<Document> searchSimilarityAnalysisResult(String problemStatement, Long userId) {
        return analysisResultVectorStore.similaritySearch(SearchRequest.builder()
                .query(problemStatement)
                .similarityThreshold(0.8)
                .filterExpression(new FilterExpressionBuilder().eq("userId", userId).build())
                .build());
    }
}