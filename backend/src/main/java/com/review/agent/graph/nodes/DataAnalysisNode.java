package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.review.agent.entity.dto.NodeExecuteDto;
import com.review.agent.service.PromptService;
import com.review.agent.service.SseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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

        // 解析状态
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
        @SuppressWarnings("unchecked")
        List<NodeExecuteDto> nodeDtoList = (List<NodeExecuteDto>) state.value("nodeResult")
                .orElseThrow(() -> new IllegalArgumentException("nodeDtoList is null"));

        sseService.sendLog(userId, "🔍 开始分析文件中的每个会话内容...");


        for (NodeExecuteDto result : nodeDtoList) {
            // 获取系统提示词
            String systemPrompt = getSystemPrompt(result.getSubTagName());

            // 调用AI
            AiAnalysisResult response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(result.getSessionContent())
                    .call()
                    .entity(AiAnalysisResult.class);

            // TODO 错误处理
            if (response == null) {
                log.info("AI 分析失败，fileId={}", fileId);
                result.setStatus(ANALYSIS_STATUS_ERROR);
            } else {
                result.setProblemStatement(response.problem());
                result.setSolution(response.analysisReport());
                result.setStatus(ANALYSIS_STATUS_PROCESSED);
            }
        }

        return Map.of("nodeResult", nodeDtoList);
    }

    /**
     * 根据标签获取系统提示词
     * @param subTagName 会话标签
     * @return 系统提示词
     */
    private String getSystemPrompt(String subTagName) {
        String nameUpperCase= subTagName.toUpperCase();
        if (nameUpperCase.contains("思维拓展")) {
            return promptService.getExtensionAnalysisPrompt("");
        } else if (nameUpperCase.contains("BUG")) {
            return promptService.getBugAnalysisPrompt("");
        } else {
            return promptService.getAnalysisPrompt("");
        }
    }

    record AiAnalysisResult(String problem, String analysisReport) {
    }

}
