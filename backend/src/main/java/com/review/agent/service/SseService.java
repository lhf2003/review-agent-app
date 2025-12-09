package com.review.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 服务，用于管理连接和推送消息
 */
@Slf4j
@Service
public class SseService {

    // 存储用户ID到Emitter的映射，支持一个用户多个连接（这里简单起见先只存一个最新的，或者用 Map<Long, List<SseEmitter>>）
    // 为了简单，我们假设一个用户同一时间只关注一个分析任务，或者广播给该用户的所有连接
    // 这里使用 ConcurrentHashMap<Long, SseEmitter>，同一个用户的新连接会覆盖旧连接
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createConnection(Long userId) {
        // 设置超时时间，0表示不过期
        SseEmitter emitter = new SseEmitter(0L);
        
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", userId);
            emitters.remove(userId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for user: {}", userId);
            emitters.remove(userId);
        });

        emitter.onError((e) -> {
            log.error("SSE connection error for user: {}", userId, e);
            emitters.remove(userId);
        });

        return emitter;
    }

    /**
     * 发送日志消息给指定用户
     * @param userId 用户ID
     * @param message 消息内容
     */
    public void sendLog(Long userId, String message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name("log").data(message));
            } catch (IOException e) {
                log.error("Failed to send log to user: {}", userId, e);
                emitters.remove(userId);
            }
        }
    }
}
