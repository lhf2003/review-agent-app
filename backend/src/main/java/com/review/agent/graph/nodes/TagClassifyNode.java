package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.QuestionClassifierNode;
import com.review.agent.entity.DataInfo;
import com.review.agent.entity.Tag;
import com.review.agent.service.DataInfoService;
import com.review.agent.service.PromptService;
import com.review.agent.service.TagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    public Map<String, Object> apply(OverAllState state) throws Exception {
        log.info("======TagClassifyNode apply start======");
//        QuestionClassifierNode questionType = new QuestionClassifierNode.Builder()
//                .chatClient(chatClient)
//                .inputTextKey("solution")
//                .classificationInstructions(List.of("Java", "数据库"))
//                .categories(List.of("和Java相关则分类为Java，和数据库相关则分类为数据库"))
//                .outputKey("question_type")
//                .build();
//        Map<String, Object> result = questionType.apply(state);

        String solution = state.value("solution").get().toString();
        String userId = state.value("userId").get().toString();

        Map<String, Long> nameToIdMap = new HashMap<>();
        String categories = buildCategories(Long.parseLong(userId), nameToIdMap);
        String systemPrompt = promptService.getClassifyPrompt(categories);

        AiAnalysisResult result = chatClient.prompt()
                .system(systemPrompt)
                .user(solution)
                .call()
                .entity(AiAnalysisResult.class);

        if (result == null) {
            log.info("AI 分类结果为空");
            return Map.of();
        }

        Long tagId = nameToIdMap.get(result.category());
        return Map.of("tagId", tagId,
                "category", result.category(),
                "keywords", result.keywords());
    }

    /**
     * 构建分类类别
     * @param userId 用户ID
     * @return 分类类别
     */
    private String buildCategories(Long userId, Map<String, Long> nameToIdMap) {
        List<Tag> tagList = tagService.findAllByUserId(userId);
        StringBuilder stringBuilder = new StringBuilder();
        for (Tag tag : tagList) {
            nameToIdMap.put(tag.getName(), tag.getId());
            stringBuilder.append(tag.getName());
            if (StringUtils.hasText(tag.getDescription())) {
                stringBuilder.append("：").append(tag.getDescription());
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }


    record AiAnalysisResult(String category, List<String> keywords) {
    }

}