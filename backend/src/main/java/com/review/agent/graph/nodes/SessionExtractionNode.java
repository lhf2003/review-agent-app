package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.review.agent.service.PromptService;
import com.review.agent.service.SseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 会话提取节点：提取出不同会话的上下文（将一个文件内容提取出不同会话）
 */
@Slf4j
@Component
public class SessionExtractionNode implements NodeAction {
    @Resource
    private PromptService promptService;

    @Resource(name = "extractChatClient")
    private ChatClient chatClient;
    @Resource
    private SseService sseService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("======SessionExtractionNode apply start======");

        Object fileId = state.value("fileId").get();
        String content = state.value("content").get().toString();
        String userId = state.value("userId").get().toString();

        sseService.sendLog(Long.parseLong(userId), "🤔 拆分文件中...正在计算文件会话数量" );

        String systemPrompt = promptService.getSessionExtractionPrompt("");

        String result = chatClient.prompt()
                .system(systemPrompt)
                .user(content)
                .call()
                .content();

        result = formatResult(result);
        if (result == null) {
            log.info("AI 会话提取失败，fileId={}", fileId);
            return Map.of();
        }

        return Map.of("sessionList", result);
    }

    public String formatResult(String result) {
        if (result == null) {
            return "";
        }
        return result.replaceAll("^\\s*```json\\s*", "")   // 去除开头 ```json
                .replaceAll("^\\s*```\\s*", "")       // 或者可能是 ``` 开头
                .replaceAll("\\s*```\\s*$", "")       // 去除结尾 ```
                .trim();
    }

}