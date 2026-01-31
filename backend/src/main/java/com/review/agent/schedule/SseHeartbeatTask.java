package com.review.agent.schedule;

import com.review.agent.common.sse.SseConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SSE 心跳定时任务
 * 定期向所有 SSE 连接发送心跳，以检测连接是否仍然活跃
 *
 * @author Review Agent
 * @since 2026-01-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseHeartbeatTask {

    private final SseConnectionManager connectionManager;

    /**
     * 发送心跳到所有 SSE 连接
     * 每 30 秒执行一次
     */
    @Scheduled(fixedRate = 30000) // 30 秒
    public void sendHeartbeat() {
        int connectionCount = connectionManager.getConnectionCount();
        if (connectionCount == 0) {
            log.debug("当前没有 SSE 连接，跳过心跳发送");
            return;
        }

        log.debug("开始发送 SSE 心跳，当前连接数：{}", connectionCount);
        connectionManager.sendHeartbeatToAll();
    }
}
