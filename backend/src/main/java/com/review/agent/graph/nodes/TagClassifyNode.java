package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.review.agent.entity.dto.MultiDimensionTagResult;
import com.review.agent.entity.dto.NodeExecuteDto;
import com.review.agent.entity.pojo.Tag;
import com.review.agent.entity.pojo.TagDimension;
import com.review.agent.graph.utils.NodeRetryHelper;
import com.review.agent.service.PromptService;
import com.review.agent.service.SseService;
import com.review.agent.service.TagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 多维度标签分类节点
 * 支持技术领域、思维范式、难度等级、应用场景四个维度的自动识别
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

    @Resource(name = "paradigmChatClient")
    private ChatClient paradigmChatClient;

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

        sseService.sendLog(userId, "正在进行多维度标签分类...");

        @SuppressWarnings("unchecked")
        List<NodeExecuteDto> nodeDtoList = (List<NodeExecuteDto>) state.value("nodeResult")
                .orElseThrow(() -> new IllegalArgumentException("nodeDtoList is null"));

        // 构建各维度的分类数据
        DimensionContext dimensionContext = buildDimensionContext(userId);

        int successCount = 0;
        int failureCount = 0;

        for (NodeExecuteDto result : nodeDtoList) {
            try {
                // 1. 技术领域分类（原有功能）
                TechDomainResult techResult = classifyTechDomain(result, dimensionContext);

                // 2. 思维范式识别（新增功能）
                List<ThinkingParadigmResult> paradigmResults = classifyThinkingParadigms(result, dimensionContext);

                // 3. 构建多维度结果
                MultiDimensionTagResult multiResult = new MultiDimensionTagResult();

                // 技术领域
                MultiDimensionTagResult.TechDomainResult techDomainResult = new MultiDimensionTagResult.TechDomainResult();
                techDomainResult.setMainTagId(techResult.mainTagId);
                techDomainResult.setMainTagName(techResult.mainTagName);
                techDomainResult.setSubTagIds(techResult.subTagIds);
                techDomainResult.setSubTagNames(techResult.subTagNames);
                techDomainResult.setRecommends(techResult.recommends);
                multiResult.setTechDomain(techDomainResult);

                // 思维范式
                List<MultiDimensionTagResult.ThinkingParadigmResult> paradigmResultList = new ArrayList<>();
                for (ThinkingParadigmResult pr : paradigmResults) {
                    MultiDimensionTagResult.ThinkingParadigmResult pResult = new MultiDimensionTagResult.ThinkingParadigmResult();
                    pResult.setParadigmCode(pr.paradigmCode);
                    pResult.setParadigmName(pr.paradigmName);
                    pResult.setConfidence(pr.confidence);
                    pResult.setApplication(pr.application);
                    pResult.setKeyInsight(pr.keyInsight);
                    pResult.setTagId(dimensionContext.paradigmCodeToIdMap.get(pr.paradigmCode));
                    paradigmResultList.add(pResult);
                }
                multiResult.setThinkingParadigms(paradigmResultList);

                // 思维质量和探索路径（从思维范式识别结果中提取）
                if (!paradigmResults.isEmpty()) {
                    multiResult.setThinkingQuality(paradigmResults.get(0).thinkingQuality);
                    multiResult.setExplorationPath(paradigmResults.get(0).explorationPath);
                }

                // 4. 更新结果
                updateNodeExecuteDto(result, multiResult);

                successCount++;
                log.debug("TagClassifyNode 成功分类会话: sessionStart={}, sessionEnd={}, techDomain={}, paradigms={}",
                        result.getSessionStart(), result.getSessionEnd(),
                        techResult.mainTagName,
                        paradigmResults.stream().map(p -> p.paradigmCode).toList());

            } catch (Exception e) {
                log.error("TagClassifyNode 分类失败，sessionStart={}，sessionEnd={}",
                        result.getSessionStart(), result.getSessionEnd(), e);
                // 设置默认值
                setDefaultResult(result);
                failureCount++;
            }
        }

        log.info("TagClassifyNode 完成: userId={}, 成功={}, 失败={}", userId, successCount, failureCount);
        sseService.sendLog(userId, String.format("多维度标签分类完成: 成功=%d, 失败=%d", successCount, failureCount));

        return Map.of("nodeResult", nodeDtoList);
    }

    /**
     * 技术领域分类（原有功能改造）
     */
    private TechDomainResult classifyTechDomain(NodeExecuteDto result, DimensionContext context) {
        String systemPrompt = promptService.getClassifyPrompt(context.techDomainCategories);

        AiTechDomainResult response = NodeRetryHelper.builder()
                .operation("TechDomainClassify[" + result.getSessionStart() + "-" + result.getSessionEnd() + "]")
                .chatClient(chatClient)
                .systemPrompt(systemPrompt)
                .userPrompt(result.getSessionContent())
                .execute(AiTechDomainResult.class, () -> createDefaultTechResult());

        if (response == null) {
            return new TechDomainResult();
        }

        TechDomainResult techResult = new TechDomainResult();

        // 主标签
        Long mainTagId = context.techDomainNameToIdMap.get(response.category);
        if (mainTagId != null) {
            techResult.mainTagId = mainTagId;
            techResult.mainTagName = response.category;
        }

        // 子标签
        if (response.subCategory != null) {
            for (String subTagName : response.subCategory) {
                Long subTagId = context.techDomainNameToIdMap.get(subTagName);
                if (subTagId != null) {
                    techResult.subTagIds.add(subTagId);
                    techResult.subTagNames.add(subTagName);
                }
            }
        }

        // 推荐标签
        if (response.recommends != null) {
            techResult.recommends.addAll(response.recommends);
        }

        return techResult;
    }

    /**
     * 思维范式识别（新增功能）
     */
    private List<ThinkingParadigmResult> classifyThinkingParadigms(NodeExecuteDto result, DimensionContext context) {
        String systemPrompt;
        try {
            systemPrompt = promptService.getParadigmRecognitionPrompt(context.paradigmDefinitions);
        } catch (Exception e) {
            log.warn("获取思维范式识别提示词失败，使用默认提示词", e);
            systemPrompt = buildDefaultParadigmPrompt(context.paradigmDefinitions);
        }

        AiParadigmResult response = NodeRetryHelper.builder()
                .operation("ParadigmRecognize[" + result.getSessionStart() + "-" + result.getSessionEnd() + "]")
                .chatClient(paradigmChatClient)
                .systemPrompt(systemPrompt)
                .userPrompt(result.getSessionContent())
                .execute(AiParadigmResult.class, () -> createDefaultParadigmResult());

        if (response == null || response.paradigms == null) {
            return Collections.emptyList();
        }

        List<ThinkingParadigmResult> results = new ArrayList<>();
        for (AiParadigmItem item : response.paradigms) {
            // 只返回置信度 >= 60 的范式
            if (item.confidence >= 60) {
                ThinkingParadigmResult pr = new ThinkingParadigmResult();
                pr.paradigmCode = item.code;
                pr.paradigmName = item.name;
                pr.confidence = item.confidence;
                pr.application = item.application;
                pr.keyInsight = item.keyInsight;
                pr.thinkingQuality = response.thinkingQuality;
                pr.explorationPath = response.explorationPath;
                results.add(pr);
            }
        }

        // 按置信度降序排序，最多返回3个
        results.sort((a, b) -> b.confidence.compareTo(a.confidence));
        return results.size() > 3 ? results.subList(0, 3) : results;
    }

    /**
     * 构建维度上下文
     */
    private DimensionContext buildDimensionContext(Long userId) {
        DimensionContext context = new DimensionContext();

        // 1. 技术领域分类
        StringBuilder techCategoriesBuilder = new StringBuilder();
        List<Tag> techDomainTags = tagService.findTechDomainTags(userId);
        for (Tag tag : techDomainTags) {
            context.techDomainNameToIdMap.put(tag.getName(), tag.getId());
            techCategoriesBuilder.append(tag.getName()).append("\n");

            List<Tag> subTags = tagService.findSubTagsByParentId(userId, tag.getId());
            for (Tag subTag : subTags) {
                context.techDomainNameToIdMap.put(subTag.getName(), subTag.getId());
                techCategoriesBuilder.append("- ").append(subTag.getName()).append("\n");
            }
        }
        context.techDomainCategories = techCategoriesBuilder.toString();

        // 2. 思维范式定义
        StringBuilder paradigmBuilder = new StringBuilder();
        List<Tag> paradigmTags = tagService.findThinkingParadigmTags();
        for (Tag tag : paradigmTags) {
            if (tag.getParadigmCode() != null && !tag.getParadigmCode().isEmpty()) {
                context.paradigmCodeToIdMap.put(tag.getParadigmCode(), tag.getId());
                paradigmBuilder.append(String.format("- %s (%s): %s\n",
                        tag.getName(), tag.getParadigmCode(),
                        tag.getDescription() != null ? tag.getDescription() : ""));
            }
        }
        context.paradigmDefinitions = paradigmBuilder.toString();

        return context;
    }

    /**
     * 更新NodeExecuteDto结果
     */
    private void updateNodeExecuteDto(NodeExecuteDto result, MultiDimensionTagResult multiResult) {
        // 技术领域（兼容旧字段）
        if (multiResult.getTechDomain() != null) {
            result.setTagId(multiResult.getTechDomain().getMainTagId());
            result.setRecommends(String.join(",", multiResult.getTechDomain().getRecommends()));
            result.setSubTagId(multiResult.getTechDomain().getSubTagIds().stream()
                    .map(String::valueOf).reduce((a, b) -> a + "," + b).orElse(""));
            result.setSubTagName(String.join(",", multiResult.getTechDomain().getSubTagNames()));
        }

        // 思维范式（新字段）
        result.setMultiDimensionResult(multiResult);
    }

    /**
     * 设置默认结果
     */
    private void setDefaultResult(NodeExecuteDto result) {
        result.setRecommends("");
        result.setSubTagId("");
        result.setSubTagName("");
        result.setMultiDimensionResult(new MultiDimensionTagResult());
    }

    /**
     * 构建默认范式提示词（降级策略）
     */
    private String buildDefaultParadigmPrompt(String paradigms) {
        return """
                你是一名思维范式识别专家。请分析用户与AI的对话，识别其中使用的思维范式。

                可选的思维范式：
                ${paradigms}

                请输出JSON格式：
                {
                  "paradigms": [
                    {
                      "code": "范式编码",
                      "name": "范式名称",
                      "confidence": 85,
                      "application": "该范式在对话中的具体应用描述",
                      "keyInsight": "使用此范式获得的关键洞察"
                    }
                  ],
                  "explorationPath": "用户引导AI解决问题的整体路径描述",
                  "thinkingQuality": "high|medium|low",
                  "alternativeApproaches": ["本可以用但未用的其他思路"]
                }
                """.replace("${paradigms}", paradigms);
    }

    // ========== 内部数据结构 ==========

    private static class DimensionContext {
        // 技术领域
        Map<String, Long> techDomainNameToIdMap = new HashMap<>();
        String techDomainCategories;

        // 思维范式
        Map<String, Long> paradigmCodeToIdMap = new HashMap<>();
        String paradigmDefinitions;
    }

    private static class TechDomainResult {
        Long mainTagId;
        String mainTagName;
        List<Long> subTagIds = new ArrayList<>();
        List<String> subTagNames = new ArrayList<>();
        List<String> recommends = new ArrayList<>();
    }

    private static class ThinkingParadigmResult {
        String paradigmCode;
        String paradigmName;
        Integer confidence;
        String application;
        String keyInsight;
        String thinkingQuality;
        String explorationPath;
    }

    // ========== AI响应记录 ==========

    record AiTechDomainResult(String category, List<String> subCategory, List<String> recommends) {
    }

    record AiParadigmResult(List<AiParadigmItem> paradigms, String explorationPath, String thinkingQuality,
                            List<String> alternativeApproaches) {
    }

    record AiParadigmItem(String code, String name, Integer confidence, String application, String keyInsight) {
    }

    private AiTechDomainResult createDefaultTechResult() {
        return new AiTechDomainResult("未分类", Collections.emptyList(), Collections.emptyList());
    }

    private AiParadigmResult createDefaultParadigmResult() {
        return new AiParadigmResult(Collections.emptyList(), "", "medium", Collections.emptyList());
    }
}
