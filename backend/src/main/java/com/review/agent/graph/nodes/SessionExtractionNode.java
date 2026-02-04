package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.review.agent.entity.dto.NodeExecuteDto;
import com.review.agent.service.PromptService;
import com.review.agent.service.SseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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

        // 获取系统提示词
        String systemPrompt = promptService.getSessionExtractionPrompt("");

        // 调用AI
        String result = chatClient.prompt()
                .system(systemPrompt)
                .user(originalContent)
                .call()
                .content();

        result = formatResult(result);
        if (result == null) {
            log.info("AI 会话提取失败，fileId={}", fileId);
            return Map.of();
        }

        // 构建结果列表
        List<NodeExecuteDto> nodeDtoList = buildNodeExecuteList(result, originalContent, userId, fileId);
        return Map.of("nodeResult", nodeDtoList);
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
     * 构建节点执行结果列表
     * @param result 会话提取结果
     * @param userId 用户ID
     * @param fileId 文件ID
     * @return 节点执行结果列表
     */
    private List<NodeExecuteDto> buildNodeExecuteList(String result, String originalContent, Long userId, Long fileId) {
        List<NodeExecuteDto> nodeDtoList = new ArrayList<>();

        JSONArray jsonArray = JSON.parseArray(result);
        for (Object session : jsonArray) {
            JSONObject sessionJson = (JSONObject) session;
            int startIndex = sessionJson.getIntValue("startIndex");
            int endIndex = sessionJson.getIntValue("endIndex");
            NodeExecuteDto nodeExecute = new NodeExecuteDto();
            nodeExecute.setSessionStart(startIndex);
            nodeExecute.setSessionEnd(endIndex);
            nodeExecute.setUserId(userId);
            nodeExecute.setFileId(fileId);

            handleSessionContent(originalContent, nodeExecute);

            nodeDtoList.add(nodeExecute);
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