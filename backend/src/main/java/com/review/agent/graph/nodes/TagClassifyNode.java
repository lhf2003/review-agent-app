package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.review.agent.entity.AnalysisResult;
import com.review.agent.entity.AnalysisTag;
import com.review.agent.entity.MainTag;
import com.review.agent.entity.SubTag;
import com.review.agent.service.PromptService;
import com.review.agent.service.TagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
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

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("======TagClassifyNode apply start======");

        String userId = state.value("userId").get().toString();
        @SuppressWarnings("unchecked")
        List<AnalysisResult> analysisResultList = (List<AnalysisResult>) state.value("analysisResultList").get();

        Map<String, Long> nameToIdMap = new HashMap<>();
        String categories = buildCategories(Long.parseLong(userId), nameToIdMap);
        String systemPrompt = promptService.getClassifyPrompt(categories);

        List<AnalysisTag> analysisTagList = new ArrayList<>();
        for (AnalysisResult result : analysisResultList) {
            AiAnalysisResult response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(result.getSolution())
                    .call()
                    .entity(AiAnalysisResult.class);

            if (response == null) {
                log.info("AI 分类标签失败，fileId：{},sessionStart：{}，sessionEnd：{}，",
                        result.getFileId(), result.getSessionStart(), result.getSessionEnd());
                continue;
            }
            AnalysisTag analysisTag = new AnalysisTag();
            analysisTag.setTagId(nameToIdMap.get(response.category()));
            List<String> subTagIdList = response.subCategory().stream()
                    .filter(nameToIdMap::containsKey)
                    .map(nameToIdMap::get)
                    .map(String::valueOf)
                    .toList();
            analysisTag.setSubTagId(String.join(",", subTagIdList));
            analysisTagList.add(analysisTag);
        }

        return Map.of("analysisTagList", analysisTagList, "analysisResultList", analysisResultList);
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