package com.review.agent.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 连接管理器
 * 用于管理 SSE（Server-Sent Events）连接，包括连接限制、心跳检测、异常处理
 *
 * @author Review Agent
 * @since 2026-01-31
 */
@Slf4j
@Component
public class SseConnectionManager {

    // 存储所有 SSE 连接：userId -> SseEmitter
    private final Map<Long, SseEmitter> connections = new ConcurrentHashMap<>();

    // 最大连接数限制（防止资源耗尽）
    private static final int MAX_CONNECTIONS = 1000;

    /**
     * 添加 SSE 连接
     *
     * @param userId 用户 ID
     * @param emitter SseEmitter 实例
     * @return true-添加成功，false-连接数已达上限
     */
    public boolean addConnection(Long userId, SseEmitter emitter) {
        // 检查连接数限制
        if (connections.size() >= MAX_CONNECTIONS) {
            log.warn("SSE 连接数已达上限，拒绝新连接，当前连接数：{}", connections.size());
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("Server is busy, too many connections"));
                emitter.complete();
            } catch (IOException e) {
                log.error("Failed to send error message to user: {}", userId, e);
            }
            return false;
        }

        // 移除旧连接（如果存在）
        removeConnection(userId);

        // 添加新连接
        connections.put(userId, emitter);

        // 设置连接完成、超时、错误时的回调
        emitter.onCompletion(() -> {
            log.info("SSE 连接已完成，userId={}，当前连接数：{}", userId, connections.size());
            removeConnection(userId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时，userId={}，当前连接数：{}", userId, connections.size());
            removeConnection(userId);
        });

        emitter.onError((e) -> {
            log.error("SSE 连接发生错误，userId={}，当前连接数：{}", userId, connections.size(), e);
            removeConnection(userId);
        });

        log.info("添加 SSE 连接成功，userId={}，当前连接数：{}", userId, connections.size());
        return true;
    }

    /**
     * 移除 SSE 连接
     *
     * @param userId 用户 ID
     */
    public void removeConnection(Long userId) {
        SseEmitter removed = connections.remove(userId);
        if (removed != null) {
            log.debug("移除 SSE 连接，userId={}，当前连接数：{}", userId, connections.size());
        }
    }

    /**
     * 获取指定用户的连接
     *
     * @param userId 用户 ID
     * @return SseEmitter 实例，如果不存在则返回 null
     */
    public SseEmitter getConnection(Long userId) {
        return connections.get(userId);
    }

    /**
     * 获取当前连接数
     *
     * @return 连接数
     */
    public int getConnectionCount() {
        return connections.size();
    }

    /**
     * 发送事件到指定用户
     *
     * @param userId 用户 ID
     * @param name 事件名称
     * @param data 事件数据
     * @return true-发送成功，false-发送失败
     */
    public boolean sendEvent(Long userId, String name, Object data) {
        SseEmitter emitter = getConnection(userId);
        if (emitter == null) {
            log.warn("用户的 SSE 连接不存在，userId={}", userId);
            return false;
        }

        try {
            emitter.send(SseEmitter.event().name(name).data(data));
            return true;
        } catch (IOException e) {
            log.error("发送事件失败，userId={}，eventName={}", userId, name, e);
            removeConnection(userId);
            return false;
        }
    }

    /**
     * 发送心跳到所有连接
     * 定期调用以检测连接是否仍然活跃
     */
    public void sendHeartbeatToAll() {
        if (connections.isEmpty()) {
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (Map.Entry<Long, SseEmitter> entry : connections.entrySet()) {
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data("ping")
                    .id(String.valueOf(System.currentTimeMillis())));
                successCount++;
            } catch (IOException e) {
                log.warn("发送心跳失败，userId={}", userId, e);
                removeConnection(userId);
                failureCount++;
            }
        }

        log.debug("心跳发送完成，成功={}，失败={}，当前连接数：{}", successCount, failureCount, connections.size());
    }

    /**
     * 清理所有连接
     * 通常在应用关闭时调用
     */
    public void closeAllConnections() {
        log.info("开始清理所有 SSE 连接，当前连接数：{}", connections.size());

        int closedCount = 0;
        for (Map.Entry<Long, SseEmitter> entry : connections.entrySet()) {
            Long userId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                emitter.complete();
                closedCount++;
            } catch (Exception e) {
                log.error("关闭连接失败，userId={}", userId, e);
            }
        }

        connections.clear();
        log.info("清理所有 SSE 连接完成，关闭了 {} 个连接", closedCount);
    }
}
