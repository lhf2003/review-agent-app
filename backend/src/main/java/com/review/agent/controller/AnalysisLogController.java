package com.review.agent.controller;

import com.review.agent.service.SseService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 分析日志接口
 */
@RestController
@RequestMapping("/analysis/log")
public class AnalysisLogController {

    @Resource
    private SseService sseService;

    /**
     * 建立SSE连接，接收日志推送
     * @return SseEmitter
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs(@RequestHeader(value = "userId", required = false) Long userId) {
        return sseService.createConnection(userId);
    }
}
