package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.review.agent.entity.AnalysisResult;
import com.review.agent.service.PromptService;
import com.review.agent.service.SseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.review.agent.common.constant.CommonConstant.ANALYSIS_STATUS_ERROR;
import static com.review.agent.common.constant.CommonConstant.ANALYSIS_STATUS_PROCESSED;

/**
 * 数据分析节点
 */
@Slf4j
@Component
public class DataAnalysisNode implements NodeAction {

    @Resource
    private PromptService promptService;

    @Resource(name = "analysisChatClient")
    private ChatClient chatClient;

    @Resource
    private SseService sseService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("======DataAnalysisNode apply start======");
        Object optional = state.value("userId").orElseThrow(() -> new IllegalArgumentException("userId is null"));
        Long userId = null;
        if (optional instanceof Long l) {
            userId = l;
        } else if (optional instanceof List<?> strings) {
            userId = Long.parseLong(strings.get(1).toString());
        }
        Object optionalFileId = state.value("fileId").orElseThrow(() -> new IllegalArgumentException("fileId is null"));
        Long fileId = null;
        if (optionalFileId instanceof Long l) {
            fileId = l;
        } else if (optionalFileId instanceof List<?> strings) {
            fileId = Long.parseLong(strings.get(1).toString());
        }
        String content = state.value("content").get().toString();
        String sessionList = state.value("sessionList").get().toString();
        sseService.sendLog(userId, "🔍 开始分析文件中的每个会话内容..." );

        String systemPrompt = promptService.getAnalysisPrompt("");

        JSONArray jsonArray = JSON.parseArray(sessionList);
        List<AnalysisResult> analysisResultList = new ArrayList<>();
        for (Object o : jsonArray) {
            AnalysisResult analysisResult = new AnalysisResult();
            analysisResult.setUserId(userId);
            analysisResult.setFileId(fileId);

            handleData(analysisResult, content, (JSONObject) o);

            AiAnalysisResult result = chatClient.prompt()
                    .system(systemPrompt)
                    .user(analysisResult.getSessionContent())
                    .call()
                    .entity(AiAnalysisResult.class);

            // TODO 错误处理
            if (result == null) {
                log.info("AI 分析失败，fileId={}", fileId);
                analysisResult.setStatus(ANALYSIS_STATUS_ERROR);
            } else {
                analysisResult.setProblemStatement(result.problem());
                analysisResult.setSolution(result.analysisReport());
                analysisResult.setStatus(ANALYSIS_STATUS_PROCESSED);
            }
            analysisResult.setCreatedTime(new Date());
            analysisResultList.add(analysisResult);
        }

        return Map.of("analysisResultList", analysisResultList);
    }

    /**
     * 处理数据，提取指定会话内容
     * @param analysisResult 分析结果实体
     * @param content 文件内容
     * @param jsonObject 会话信息json对象
     */
    private void handleData(AnalysisResult analysisResult, String content, JSONObject jsonObject) {
        Integer startIndex = jsonObject.getInteger("startIndex");
        Integer endIndex = jsonObject.getInteger("endIndex");
        // 提取指定会话内容
        String regex = "(?m)^\\s*(?=#\\s+\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})";
        String[] blocks = content.split(regex);
        List<String> blockList = Arrays.stream(blocks).filter(block -> !block.trim().isEmpty()).toList();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = startIndex - 1; i < endIndex; i++) {
            stringBuilder.append(blockList.get(i)).append("\n");
        }

        analysisResult.setSessionStart(startIndex);
        analysisResult.setSessionEnd(endIndex);
        analysisResult.setSessionContent(stringBuilder.toString());
    }


    record AiAnalysisResult(String problem, String analysisReport) {
    }

}
