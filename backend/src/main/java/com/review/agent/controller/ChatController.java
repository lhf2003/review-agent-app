package com.review.agent.controller;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Resource
    private ChatService chatService;

    /**
     * 闲聊
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestHeader("userId") Long userId, @RequestParam("request") String request) throws GraphRunnerException {
        return chatService.chat(userId, request);
    }

    /**
     * 基于分析结果聊天
     */
    @PostMapping(path = "/with-analysis", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatWithAnalysisResult(@RequestHeader("userId") Long userId, @RequestParam("request") String request) throws GraphRunnerException {
        return chatService.chatWithAnalysisResult(userId, request);
    }

    /**
     * 清空上下文
     */
    @GetMapping("/clear")
    public BaseResponse<Void> clearContext(@RequestHeader("userId") Long userId) {
        chatService.clearContext(userId);
        return ResultUtil.success();
    }

}