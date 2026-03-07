package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.review.agent.entity.dto.NodeExecuteDto;
import com.review.agent.entity.pojo.Tag;
import com.review.agent.graph.utils.NodeRetryHelper;
import com.review.agent.repository.TagRepository;
import com.review.agent.service.ParadigmVisualizationService;
import com.review.agent.service.PromptService;
import com.review.agent.service.SseService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 思维范式识别节点
 * 识别对话中使用的思考方式和解决问题的方法论
 */
@Slf4j
@Component
public class ThinkingParadigmNode implements NodeAction {

    @Resource
    private PromptService promptService;

    @Resource
    private TagRepository tagRepository;

    @Resource(name = "classifyChatClient")
    private ChatClient chatClient;

    @Resource
    private SseService sseService;

    @Resource
    private ParadigmVisualizationService visualizationService;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("======ThinkingParadigmNode apply start======");

        // 解析状态
        Object optional = state.value("userId").orElseThrow(() -> new IllegalArgumentException("userId is null"));
        Long userId = null;
        if (optional instanceof Long l) {
            userId = l;
        } else if (optional instanceof List<?> strings) {
            userId = Long.parseLong(strings.get(1).toString());
        }

        sseService.sendLog(userId, "正在识别思维范式...");

        @SuppressWarnings("unchecked")
        List<NodeExecuteDto> nodeDtoList = (List<NodeExecuteDto>) state.value("nodeResult")
                .orElseThrow(() -> new IllegalArgumentException("nodeDtoList is null"));

        // 获取思维范式标签列表
        List<Tag> paradigmTags = tagRepository.findAllThinkingParadigms();
        String paradigmDefinitions = buildParadigmDefinitions(paradigmTags);

        // 构建范式名称到ID的映射
        Map<String, Long> paradigmCodeToIdMap = paradigmTags.stream()
                .filter(t -> t.getParadigmCode() != null)
                .collect(Collectors.toMap(Tag::getParadigmCode, Tag::getId));

        // 获取系统提示词
        String systemPrompt;
        try {
            systemPrompt = promptService.getParadigmRecognitionPrompt(paradigmDefinitions);
        } catch (Exception e) {
            log.error("获取思维范式识别提示词失败", e);
            systemPrompt = buildDefaultPrompt(paradigmTags);
        }

        int successCount = 0;
        int failureCount = 0;

        for (NodeExecuteDto result : nodeDtoList) {
            // 使用重试机制调用AI
            ParadigmRecognitionResult response = NodeRetryHelper.builder()
                    .operation("ThinkingParadigm[" + result.getSessionStart() + "-" + result.getSessionEnd() + "]")
                    .chatClient(chatClient)
                    .systemPrompt(systemPrompt)
                    .userPrompt(result.getSessionContent())
                    .execute(ParadigmRecognitionResult.class, () -> createDefaultResult());

            if (response == null || response.paradigms == null || response.paradigms.isEmpty()) {
                log.warn("ThinkingParadigmNode 未识别出思维范式，sessionStart={}，sessionEnd={}",
                        result.getSessionStart(), result.getSessionEnd());
                result.setThinkingParadigmIds("");
                result.setThinkingParadigms("");
                result.setExplorationPath("");
                result.setThinkingQuality("unknown");
                failureCount++;
            } else {
                // 构建范式ID列表
                List<Long> paradigmIdList = response.paradigms.stream()
                        .map(p -> paradigmCodeToIdMap.get(p.code))
                        .filter(id -> id != null)
                        .toList();

                List<String> paradigmNameList = response.paradigms.stream()
                        .map(ParadigmInfo::name)
                        .toList();

                result.setThinkingParadigmIds(paradigmIdList.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));
                result.setThinkingParadigms(String.join(",", paradigmNameList));
                result.setExplorationPath(response.explorationPath);
                result.setThinkingQuality(response.thinkingQuality);

                // 保存详细的范式应用信息（用于后续展示）
                Map<String, Object> paradigmDetails = new HashMap<>();
                paradigmDetails.put("paradigms", response.paradigms);
                paradigmDetails.put("alternativeApproaches", response.alternativeApproaches);
                result.setParadigmDetails(paradigmDetails);

                successCount++;
                log.debug("ThinkingParadigmNode 成功识别范式: sessionStart={}, sessionEnd={}, paradigms={}",
                        result.getSessionStart(), result.getSessionEnd(), paradigmNameList);
            }
        }

        log.info("ThinkingParadigmNode 完成: userId={}, 成功={}, 失败={}", userId, successCount, failureCount);

        // 提取并保存思维范式可视化（如果识别到范式）
        if (successCount > 0) {
            try {
                Object analysisResultIdObj = state.value("analysisResultId").orElse(null);
                if (analysisResultIdObj != null) {
                    Long analysisResultId = null;
                    if (analysisResultIdObj instanceof Long) {
                        analysisResultId = (Long) analysisResultIdObj;
                    } else if (analysisResultIdObj instanceof String) {
                        analysisResultId = Long.parseLong((String) analysisResultIdObj);
                    } else if (analysisResultIdObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> list = (List<String>) analysisResultIdObj;
                        if (!list.isEmpty()) {
                            analysisResultId = Long.parseLong(list.get(0));
                        }
                    }

                    if (analysisResultId != null) {
                        sseService.sendLog(userId, "正在提取思维范式可视化...");
                        visualizationService.extractAndSaveVisualizations(analysisResultId, userId, nodeDtoList);
                        log.info("思维范式可视化提取完成: analysisResultId={}", analysisResultId);
                    }
                }
            } catch (Exception e) {
                log.error("提取思维范式可视化失败: {}", e.getMessage(), e);
                // 不影响主流程
            }
        }

        return Map.of("nodeResult", nodeDtoList);
    }

    /**
     * 构建范式定义说明
     */
    private String buildParadigmDefinitions(List<Tag> paradigmTags) {
        StringBuilder sb = new StringBuilder();
        for (Tag tag : paradigmTags) {
            sb.append("- ").append(tag.getParadigmCode())
              .append(" (").append(tag.getName()).append(")")
              .append(": ").append(tag.getDescription())
              .append("，适用场景：").append(tag.getWhenToUse())
              .append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建默认提示词（降级策略）
     */
    private String buildDefaultPrompt(List<Tag> paradigmTags) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个思维范式识别专家。请从以下范式中识别对话中使用的思维方法：\n\n");
        sb.append(buildParadigmDefinitions(paradigmTags));
        sb.append("\n请返回JSON格式：{\"paradigms\":[{\"code\":\"编码\",\"name\":\"名称\",\"confidence\":置信度(1-100),\"application\":\"应用描述\",\"keyInsight\":\"关键洞察\"}],\"explorationPath\":\"探索路径\",\"thinkingQuality\":\"high|medium|low\",\"alternativeApproaches\":[\"其他思路\"]}");
        return sb.toString();
    }

    /**
     * 创建默认结果（降级策略）
     */
    private ParadigmRecognitionResult createDefaultResult() {
        return new ParadigmRecognitionResult(
                Collections.emptyList(),
                "",
                "unknown",
                Collections.emptyList()
        );
    }

    // 范式识别结果记录
    public record ParadigmRecognitionResult(
            List<ParadigmInfo> paradigms,
            String explorationPath,
            String thinkingQuality,
            List<String> alternativeApproaches
    ) {
    }

    // 范式信息记录
    public record ParadigmInfo(
            String code,
            String name,
            Integer confidence,
            String application,
            String keyInsight
    ) {
    }
}
