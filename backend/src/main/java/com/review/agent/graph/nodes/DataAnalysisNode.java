package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.review.agent.entity.AnalysisResult;
import com.review.agent.entity.DataInfo;
import com.review.agent.service.DataInfoService;
import com.review.agent.service.PromptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 数据分析节点
 */
@Slf4j
@Component
public class DataAnalysisNode implements NodeAction {
    public static final String PROMPT_NAME = "Analysis-agent-prompt.md";

    @Resource
    private PromptService promptService;

    @Resource
    private DataInfoService dataInfoService;

    @Resource(name = "analysisChatClient")
    private ChatClient chatClient;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("======DataAnalysisNode apply start======");

        Object fileId = state.value("fileId").get();
        String content = state.value("content").get().toString();

        String systemPrompt = promptService.getAnalysisPrompt("");

        AiAnalysisResult result = chatClient.prompt()
                .system(systemPrompt)
                .user(content)
                .call()
                .entity(AiAnalysisResult.class);

        if (result == null) {
            log.info("AI 分析结果为空，fileId={}", fileId);
            return Map.of();
        }

        return Map.of("problem", result.problem(), "solution", result.solution());
    }


    record AiAnalysisResult(String problem, String solution) {
    }

}
