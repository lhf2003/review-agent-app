package com.review.agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import reactor.core.publisher.Flux;

import java.util.Optional;

@Slf4j
@Component
public class TokenLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ChatClientResponse advisedResponse = callAdvisorChain.nextCall(chatClientRequest);
        stopWatch.stop();
        ChatResponse chatResponse = Optional.ofNullable(advisedResponse.chatResponse())
                .orElseThrow(() -> new IllegalArgumentException("chatResponse is null"));
        ChatResponseMetadata metadata = chatResponse.getMetadata();
        Usage usage = metadata.getUsage();
        log.info("本次模型调用 usage: {}, 耗时: {}ms", usage, stopWatch.getTotalTimeMillis());
        return advisedResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return null;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}