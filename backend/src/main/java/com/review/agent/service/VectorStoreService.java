//package com.review.agent.service;
//
//import org.springframework.ai.document.Document;
//import org.springframework.ai.vectorstore.SearchRequest;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.stereotype.Service;
//import redis.clients.jedis.JedisPooled;
//
//import java.util.List;
//
//
//@Service
//public class VectorStoreService {
//    private final VectorStore vectorStore;
//    private final JedisPooled jedisPooled;
//
//    public VectorStoreService(VectorStore vectorStore, JedisPooled jedisPooled) {
//        this.vectorStore = vectorStore;
//        this.jedisPooled = jedisPooled;
//
//    }
//
//    /**
//     * 搜索一个相似文档
//     * @param request 用户文本
//     * @return 相似文档
//     */
//    public Document searchOne(String request) {
//        List<Document> documentList = vectorStore.similaritySearch(SearchRequest.builder()
//                .query(request)
//                .build());
//        if (documentList.isEmpty()) {
//            return null;
//        }
//        return documentList.get(0);
//    }
//
//    /**
//     * 添加一个文档到向量数据库
//     * @param document 文档
//     */
//    public void addOne(Document document) {
//        vectorStore.add(List.of(document));
//    }
//}