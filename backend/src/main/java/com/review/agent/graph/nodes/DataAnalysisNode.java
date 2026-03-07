package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.review.agent.entity.dto.NodeExecuteDto;
import com.review.agent.graph.utils.NodeRetryHelper;
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
        Object originalContent = state.value("originalContent").orElseThrow(() -> new IllegalArgumentException("originalContent is null"));

        @SuppressWarnings("unchecked")
        NodeExecuteDto nodeDto = (NodeExecuteDto) state.value("nodeResult")
                .orElseThrow(() -> new IllegalArgumentException("nodeDtoList is null"));

        // 推送阶段2：AI分析中
        sseService.sendStage(userId, 2);

        // 获取系统提示词
        String systemPrompt = getSystemPrompt(nodeDto.getSubTagName());

        // 使用重试机制调用AI
        AiAnalysisResult response = NodeRetryHelper.builder()
                .operation("DataAnalysis")
                .chatClient(chatClient)
                .systemPrompt(systemPrompt)
                .userPrompt(originalContent.toString())
                .execute(AiAnalysisResult.class);

        if (response == null) {
            log.error("DataAnalysisNode AI 分析最终失败，fileId={}", fileId);
            nodeDto.setStatus(ANALYSIS_STATUS_ERROR);
            nodeDto.setProblemStatement("AI 分析服务暂时不可用，请稍后重试");
            nodeDto.setSolution("系统正在处理中，如问题持续请联系管理员");
        } else {
            nodeDto.setProblemStatement(response.problem());
            nodeDto.setSolution(response.analysisReport());
            nodeDto.setStatus(ANALYSIS_STATUS_PROCESSED);
        }

        return Map.of("nodeResult", nodeDto);
    }

    /**
     * 根据标签获取系统提示词
     * @param subTagName 会话标签
     * @return 系统提示词
     */
    private String getSystemPrompt(String subTagName) {
        if (subTagName == null || subTagName.isEmpty()) {
            return promptService.getAnalysisPrompt("");
        }
        String nameUpperCase = subTagName.toUpperCase();
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
