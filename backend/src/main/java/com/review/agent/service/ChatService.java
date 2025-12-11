package com.review.agent.service;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

    @Resource
    private ChatModel chatModel;
    @Resource
    private UserService userService;


    public Flux<String> chat(Long userId, String request) throws GraphRunnerException {
        ReactAgent reactAgent = ReactAgent.builder()
                .name("review-agent")
                .model(chatModel)
                .systemPrompt("你是一个专业的助手,擅长回答用户的问题")
                .build();
        Flux<NodeOutput> stream = reactAgent.stream(request);
        Flux<String> flux = Flux.create(emitter -> {
            stream.subscribe(output -> {
                if (output instanceof StreamingOutput<?> streamingOutput) {
                    // 流式输出块
                    String chunk = streamingOutput.chunk();
                    if (chunk != null && !chunk.isEmpty()) {
                        System.out.print(chunk);
                        emitter.next(chunk);
                    }
                }
            });
        });
        return flux;
    }
}