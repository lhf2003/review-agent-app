package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.review.agent.entity.pojo.MainTag;
import com.review.agent.entity.pojo.SubTag;
import com.review.agent.entity.dto.NodeExecuteDto;
import com.review.agent.graph.utils.NodeRetryHelper;
import com.review.agent.service.PromptService;
import com.review.agent.service.SseService;
import com.review.agent.service.TagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标签分类节点
 */
@Slf4j
@Component
public class TagClassifyNode implements NodeAction {
    @Resource
    private PromptService promptService;

    @Resource
    private TagService tagService;

    @Resource(name = "classifyChatClient")
    private ChatClient chatClient;
    @Resource
    private SseService sseService;


    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("======TagClassifyNode apply start======");

        // 解析状态
        Object optional = state.value("userId").orElseThrow(() -> new IllegalArgumentException("userId is null"));
        Long userId = null;
        if (optional instanceof Long l) {
            userId = l;
        } else if (optional instanceof List<?> strings) {
            userId = Long.parseLong(strings.get(1).toString());
        }

        sseService.sendLog(userId, "正在匹配主标签和子标签...");

        @SuppressWarnings("unchecked")
        List<NodeExecuteDto> nodeDtoList = (List<NodeExecuteDto>) state.value("nodeResult")
                .orElseThrow(() -> new IllegalArgumentException("nodeDtoList is null"));

        Map<String, Long> nameToIdMap = new HashMap<>();
        // 获取系统提示词
        String categories = buildCategories(userId, nameToIdMap);
        String systemPrompt = promptService.getClassifyPrompt(categories);

        int successCount = 0;
        int failureCount = 0;

        for (NodeExecuteDto result : nodeDtoList) {
            // 使用重试机制调用AI
            AiAnalysisResult response = NodeRetryHelper.builder()
                    .operation("TagClassify[" + result.getSessionStart() + "-" + result.getSessionEnd() + "]")
                    .chatClient(chatClient)
                    .systemPrompt(systemPrompt)
                    .userPrompt(result.getSessionContent())
                    .execute(AiAnalysisResult.class, () -> createDefaultResult());

            if (response == null) {
                log.error("TagClassifyNode AI 分类标签最终失败，sessionStart={}，sessionEnd={}",
                        result.getSessionStart(), result.getSessionEnd());
                // 设置默认值
                result.setRecommends("");
                result.setSubTagId("");
                result.setSubTagName("");
                failureCount++;
            } else {
                // 构建结果列表
                Long mainTagId = nameToIdMap.get(response.category());
                if (mainTagId != null) {
                    result.setTagId(mainTagId);
                } else {
                    log.warn("TagClassifyNode 主标签未匹配: category={}", response.category());
                }

                result.setRecommends(response.recommends() != null ?
                        String.join(",", response.recommends()) : "");

                List<String> subTagIdList = response.subCategory().stream()
                        .filter(nameToIdMap::containsKey)
                        .map(nameToIdMap::get)
                        .map(String::valueOf)
                        .toList();
                result.setSubTagId(String.join(",", subTagIdList));
                result.setSubTagName(response.subCategory() != null ?
                        String.join(",", response.subCategory()) : "");
                successCount++;
                log.debug("TagClassifyNode 成功分类会话: sessionStart={}, sessionEnd={}, category={}",
                        result.getSessionStart(), result.getSessionEnd(), response.category());
            }
        }

        log.info("TagClassifyNode 完成: userId={}, 成功={}, 失败={}", userId, successCount, failureCount);

        return Map.of("nodeResult", nodeDtoList);
    }

    /**
     * 创建默认的 AI 分析结果（降级策略）
     */
    private AiAnalysisResult createDefaultResult() {
        return new AiAnalysisResult("未分类", Collections.emptyList(), Collections.emptyList());
    }

    /**
     * 构建分类类别
     * @param userId 用户ID
     * @return 分类类别
     */
    private String buildCategories(Long userId, Map<String, Long> nameToIdMap) {
        StringBuilder stringBuilder = new StringBuilder();
        // 主标签
        List<MainTag> mainTagList = tagService.findMainTagList(userId);
        for (MainTag mainTag : mainTagList) {
            nameToIdMap.put(mainTag.getName(), mainTag.getId());
            stringBuilder.append(mainTag.getName()).append("\n");

            // 子标签
            List<SubTag> subTagList = tagService.findSubTagListByMainTagId(userId, mainTag.getId());
            for (SubTag subTag : subTagList) {
                nameToIdMap.put(subTag.getName(), subTag.getId());
                stringBuilder.append("- ").append(subTag.getName()).append("\n");
            }
        }
        return stringBuilder.toString();
    }


    record AiAnalysisResult(String category, List<String> subCategory, List<String> recommends) {
    }

}
