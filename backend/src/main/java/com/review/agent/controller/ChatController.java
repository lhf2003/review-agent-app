package com.review.agent.controller;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {
    @Resource
    private ChatService chatService;
    @Resource
    private SecurityUtils securityUtils;

    @GetMapping("/placeholders")
    public BaseResponse<List<String>> placeholders() {
        return ResultUtil.success(List.of("你需要我的帮助吗？", "发现一个新文件，需要我分析吗？",
                "输入关键字搜索分析结果...", "试试问我关于代码的问题"));
    }

    /**
     * 闲聊
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam("request") String request) throws GraphRunnerException {
        Long userId = securityUtils.getCurrentUserId();
        return chatService.chat(userId, request);
    }

    /**
     * 基于分析结果聊天
     */
    @PostMapping(path = "/with-analysis", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatWithAnalysisResult(@RequestParam("request") String request) throws GraphRunnerException {
        Long userId = securityUtils.getCurrentUserId();
        return chatService.chatWithAnalysisResult(userId, request);
    }

    /**
     * 清空上下文
     */
    @GetMapping("/clear")
    public BaseResponse<Void> clearContext() {
        Long userId = securityUtils.getCurrentUserId();
        chatService.clearContext(userId);
        return ResultUtil.success();
    }

}