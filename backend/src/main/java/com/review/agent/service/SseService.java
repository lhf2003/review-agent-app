package com.review.agent.service;

import com.review.agent.common.sse.SseConnectionManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 服务，用于管理连接和推送消息
 */
@Slf4j
@Service
public class SseService {

    @Resource
    private SseConnectionManager connectionManager;

    /**
     * 创建 SSE 连接
     *
     * @param userId 用户 ID
     * @return SseEmitter 实例
     */
    public SseEmitter createConnection(Long userId) {
        // 设置超时时间，0 表示不过期（实际由客户端断开决定）
        SseEmitter emitter = new SseEmitter(0L);

        // 使用连接管理器管理连接
        boolean added = connectionManager.addConnection(userId, emitter);
        if (!added) {
            // 连接数已达上限，返回一个立即完成的 emitter
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("Server is busy, too many connections"));
            } catch (Exception e) {
                log.error("Failed to send error message to user: {}", userId, e);
            }
            emitter.complete();
        }

        return emitter;
    }

    /**
     * 发送日志消息给指定用户
     *
     * @param userId 用户 ID
     * @param message 消息内容
     */
    public void sendLog(Long userId, String message) {
        boolean success = connectionManager.sendEvent(userId, "log", message);
        if (!success) {
            log.warn("发送日志失败，userId={}，message={}", userId, message);
        }
    }

    /**
     * 发送阶段消息给指定用户（整型阶段）
     *
     * @param userId 用户 ID
     * @param stage 阶段编号（1, 2, 3）
     */
    public void sendStage(Long userId, Integer stage) {
        boolean success = connectionManager.sendEvent(userId, "stage", stage);
        if (!success) {
            log.warn("发送阶段失败，userId={}，stage={}", userId, stage);
        }
    }

    /**
     * 发送错误消息给指定用户
     *
     * @param userId 用户 ID
     * @param errorMessage 错误消息
     */
    public void sendError(Long userId, String errorMessage) {
        boolean success = connectionManager.sendEvent(userId, "error", errorMessage);
        if (!success) {
            log.warn("发送错误失败，userId={}，errorMessage={}", userId, errorMessage);
        }
    }

    /**
     * 获取当前连接数
     *
     * @return 连接数
     */
    public int getConnectionCount() {
        return connectionManager.getConnectionCount();
    }
}
