package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.review.agent.entity.pojo.MainTag;
import com.review.agent.entity.pojo.SubTag;
import com.review.agent.entity.dto.NodeExecuteDto;
import com.review.agent.service.PromptService;
import com.review.agent.service.SseService;
import com.review.agent.service.TagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

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

        sseService.sendLog(userId, "🏷️ 正在匹配主标签和子标签...");

        @SuppressWarnings("unchecked")
        List<NodeExecuteDto> nodeDtoList = (List<NodeExecuteDto>) state.value("nodeResult")
                .orElseThrow(() -> new IllegalArgumentException("nodeDtoList is null"));

        Map<String, Long> nameToIdMap = new HashMap<>();
        // 获取系统提示词
        String categories = buildCategories(userId, nameToIdMap);
        String systemPrompt = promptService.getClassifyPrompt(categories);

        for (NodeExecuteDto result : nodeDtoList) {
            // 调用AI
            AiAnalysisResult response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(result.getSessionContent())
                    .call()
                    .entity(AiAnalysisResult.class);

            if (response == null) {
                log.info("AI 分类标签失败，sessionStart：{}，sessionEnd：{}，", result.getSessionStart(), result.getSessionEnd());
                continue;
            }

            // 构建结果列表
            result.setTagId(nameToIdMap.get(response.category()));
            result.setRecommends(String.join(",", response.recommends()));
            List<String> subTagIdList = response.subCategory().stream()
                    .filter(nameToIdMap::containsKey)
                    .map(nameToIdMap::get)
                    .map(String::valueOf)
                    .toList();
            result.setSubTagId(String.join(",", subTagIdList));
            result.setSubTagName(String.join(",", response.subCategory()));
        }

        return Map.of("nodeResult", nodeDtoList);
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