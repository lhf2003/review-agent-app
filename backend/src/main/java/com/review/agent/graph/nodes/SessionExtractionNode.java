package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.review.agent.common.utils.JsonlUtils;
import com.review.agent.entity.dto.NodeExecuteDto;
import com.review.agent.graph.utils.NodeRetryHelper;
import com.review.agent.service.PromptService;
import com.review.agent.service.SseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // 解析状态
        String originalContent = state.value("originalContent").get().toString();
        Object optionalFileId = state.value("fileId").orElseThrow(() -> new IllegalArgumentException("fileId is null"));
        Long fileId = null;
        if (optionalFileId instanceof Long l) {
            fileId = l;
        } else if (optionalFileId instanceof List<?> strings) {
            fileId = Long.parseLong(strings.get(1).toString());
        }
        Object optional = state.value("userId").orElseThrow(() -> new IllegalArgumentException("userId is null"));
        Long userId = null;
        if (optional instanceof Long l) {
            userId = l;
        } else if (optional instanceof List<?> strings) {
            userId = Long.parseLong(strings.get(1).toString());
        }

        // 检查是否为 JSONL 格式（新格式）
        if (JsonlUtils.isJsonlFormat(originalContent)) {
            log.info("SessionExtractionNode 检测到 JSONL 格式，直接解析，fileId={}", fileId);
            List<NodeExecuteDto> nodeDtoList = JsonlUtils.parseJsonlContent(originalContent, userId, fileId);
            if (!nodeDtoList.isEmpty()) {
                log.info("SessionExtractionNode JSONL 解析完成: fileId={}, 会话数={}", fileId, nodeDtoList.size());
                return Map.of("nodeResult", nodeDtoList);
            }
            // JSONL 解析失败，降级为单个会话
            log.warn("SessionExtractionNode JSONL 解析为空，使用降级策略，fileId={}", fileId);
            return Map.of("nodeResult", createFallbackSession(originalContent, userId, fileId));
        }

        // 旧格式：使用 AI 提取会话
        log.info("SessionExtractionNode 使用 AI 提取会话，fileId={}", fileId);

        // 获取系统提示词
        String systemPrompt = promptService.getSessionExtractionPrompt("");

        // 使用重试机制调用AI
        String result = NodeRetryHelper.builder()
                .operation("SessionExtraction[fileId=" + fileId + "]")
                .chatClient(chatClient)
                .systemPrompt(systemPrompt)
                .userPrompt(originalContent)
                .executeContent(() -> "[]");  // 降级：返回空数组

        result = formatResult(result);
        if (result == null || result.isEmpty()) {
            log.error("SessionExtractionNode AI 会话提取最终失败，fileId={}", fileId);
            // 降级策略：将整个文件作为单个会话
            return Map.of("nodeResult", createFallbackSession(originalContent, userId, fileId));
        }

        // 构建结果列表
        try {
            List<NodeExecuteDto> nodeDtoList = buildNodeExecuteList(result, originalContent, userId, fileId);
            log.info("SessionExtractionNode 完成: fileId={}, 提取会话数={}", fileId, nodeDtoList.size());
            return Map.of("nodeResult", nodeDtoList);
        } catch (JSONException e) {
            log.error("SessionExtractionNode JSON 解析失败，fileId={}, error={}", fileId, e.getMessage());
            // JSON 解析失败，使用降级策略
            return Map.of("nodeResult", createFallbackSession(originalContent, userId, fileId));
        }
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

    /**
     * 创建降级会话（当 AI 提取失败时使用）
     * 将整个文件内容作为单个会话
     */
    private List<NodeExecuteDto> createFallbackSession(String originalContent, Long userId, Long fileId) {
        log.warn("SessionExtractionNode 使用降级策略，fileId={}", fileId);
        List<NodeExecuteDto> fallbackList = new ArrayList<>();
        NodeExecuteDto fallbackDto = new NodeExecuteDto();
        fallbackDto.setSessionStart(0);
        fallbackDto.setSessionEnd(originalContent.length());
        fallbackDto.setUserId(userId);
        fallbackDto.setFileId(fileId);
        fallbackDto.setSessionContent(originalContent);
        fallbackList.add(fallbackDto);
        return fallbackList;
    }


    /**
     * 构建节点执行结果列表
     * @param result 会话提取结果
     * @param userId 用户ID
     * @param fileId 文件ID
     * @return 节点执行结果列表
     */
    private List<NodeExecuteDto> buildNodeExecuteList(String result, String originalContent, Long userId, Long fileId) {
        List<NodeExecuteDto> nodeDtoList = new ArrayList<>();

        JSONArray jsonArray = JSON.parseArray(result);
        if (jsonArray == null || jsonArray.isEmpty()) {
            log.warn("SessionExtractionNode AI 返回空数组，fileId={}", fileId);
            return createFallbackSession(originalContent, userId, fileId);
        }

        for (Object session : jsonArray) {
            try {
                JSONObject sessionJson = (JSONObject) session;
                int startIndex = sessionJson.getIntValue("startIndex");
                int endIndex = sessionJson.getIntValue("endIndex");

                // 边界检查
                if (startIndex < 0 || endIndex < 0 || startIndex > endIndex) {
                    log.warn("SessionExtractionNode 无效的会话索引: startIndex={}, endIndex={}, fileId={}",
                            startIndex, endIndex, fileId);
                    continue;
                }

                NodeExecuteDto nodeExecute = new NodeExecuteDto();
                nodeExecute.setSessionStart(startIndex);
                nodeExecute.setSessionEnd(endIndex);
                nodeExecute.setUserId(userId);
                nodeExecute.setFileId(fileId);

                handleSessionContent(originalContent, nodeExecute);

                if (nodeExecute.getSessionContent() != null && !nodeExecute.getSessionContent().isEmpty()) {
                    nodeDtoList.add(nodeExecute);
                }
            } catch (Exception e) {
                log.error("SessionExtractionNode 处理会话失败: fileId={}, error={}", fileId, e.getMessage());
            }
        }

        // 如果没有提取到任何有效会话，使用降级策略
        if (nodeDtoList.isEmpty()) {
            log.warn("SessionExtractionNode 未提取到有效会话，fileId={}", fileId);
            return createFallbackSession(originalContent, userId, fileId);
        }

        return nodeDtoList;
    }

    /**
     * 提取指定会话内容
     * @param originalContent 文件内容
     * @param nodeExecute 会话信息对象
     */
    private void handleSessionContent(String originalContent, NodeExecuteDto nodeExecute) {
        Integer startIndex = nodeExecute.getSessionStart();
        Integer endIndex = nodeExecute.getSessionEnd();

        String regex = "(?ms)(^\\s*#\\s+\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}.*?)(?=(?:^\\s*#\\s+\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})|\\z)";

        Matcher matcher = Pattern.compile(regex).matcher(originalContent);
        StringBuilder stringBuilder = new StringBuilder();
        int count = 0;
        while (matcher.find()) {
            count++;
            String session = matcher.group();
            if (count == startIndex) {
                nodeExecute.setSessionStart(matcher.start());
                stringBuilder.append(session).append("\n");
            }
            if (count == endIndex) {
                nodeExecute.setSessionEnd(matcher.end());
                stringBuilder.append(session).append("\n");
            }
        }
        nodeExecute.setSessionContent(stringBuilder.toString());
    }

}
