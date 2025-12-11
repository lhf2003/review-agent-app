package com.review.agent.controller;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
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

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestHeader("userId") Long userId, @RequestParam("request") String request) throws GraphRunnerException {
        return chatService.chat(userId, request);
    }
}