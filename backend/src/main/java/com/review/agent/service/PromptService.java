package com.review.agent.service;

import com.review.agent.common.exception.ErrorCode;
import com.review.agent.common.exception.PromptProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词管理
 */
@Slf4j
@Service
public class PromptService {

    // 提示词文件列表
    private static final String[] PROMPT_FILES = {"prompts/summary-agent-prompt.md"};

    private Map<String, String> promptTemplates = new HashMap<>();

    private boolean initialized = false;

    /**
     * 初始化提示词模板
     */
    public synchronized void initialize() throws PromptProcessingException {
        if (initialized) {
            return;
        }

        try {
            // 加载所有提示词文件
            for (String fileName : PROMPT_FILES) {
                loadPromptFile(fileName);
            }

            initialized = true;
            log.info("提示词模板加载完成，共加载 {} 个模板", promptTemplates.size());
        } catch (IOException e) {
            throw new PromptProcessingException(ErrorCode.PROMPT_PROCESSING_ERROR, "加载提示词文件失败");
        }
    }

    /**
     * 加载单个提示词文件
     */
    private void loadPromptFile(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(fileName);
        if (!resource.exists()) {
            log.warn("提示词文件不存在: {}", fileName);
            return;
        }

        String content = resource.getContentAsString(StandardCharsets.UTF_8);
        parsePrompts(content, fileName);
        log.debug("加载提示词文件: {}, 解析完成", fileName);
    }

    /**
     * 解析提示词文件
     */
    private void parsePrompts(String content, String fileName) {
        // 使用正则表达式匹配提示词块
        Pattern pattern = Pattern.compile("## (.*?)\\n(.*?)(?=\\n## |$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String promptName = matcher.group(1).trim();
            String promptTemplate = matcher.group(2).trim();

            // 添加文件前缀避免重名
            String fullPromptName = buildPromptName(fileName, promptName);

            promptTemplates.put(fullPromptName, promptTemplate);
            log.debug("加载提示词: {} -> {}", fullPromptName, promptName);
        }
    }

    /**
     * 构建提示词名称，消除复杂的条件链
     */
    private String buildPromptName(String fileName, String promptName) {
        if (fileName.contains("summary")) {
            return "summary." + promptName;
        }
        if (fileName.contains("common")) {
            return "common." + promptName;
        }
        return promptName;
    }

    /**
     * 获取提示词模板
     */
    public String getPromptTemplate(String promptName) throws PromptProcessingException {
        if (!initialized) {
            try {
                initialize();
            } catch (PromptProcessingException e) {
                throw e;
            }
        }

        String template = promptTemplates.get(promptName);
        if (template == null) {
            throw new PromptProcessingException(ErrorCode.PROMPT_PROCESSING_ERROR, "未找到提示词模板: " + promptName);
        }

        return template;
    }

    /**
     * 构建提示词
     */
    public String buildPrompt(String promptName, Map<String, Object> variables) throws PromptProcessingException {
        String template = getPromptTemplate(promptName);
        return replaceVariables(template, variables);
    }

    /**
     * 替换模板变量
     */
    private String replaceVariables(String template, Map<String, Object> variables) {
        String result = template;

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }


    /**
     * 获取生成封面文案提示词
     */
    public String getXhsSummaryPrompt(String problemStatement) throws PromptProcessingException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("problemStatement", problemStatement);
        return buildPrompt("summary.小红书文案提示词", variables);
    }

    /**
     * 获取总结文本提示词
     */
    public String getTextSummaryPrompt(String problemStatement) throws PromptProcessingException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("problemStatement", problemStatement);
        return buildPrompt("summary.内容总结提示词", variables);
    }

    // ========== 通用提示词 ==========

    /**
     * 获取错误处理提示词
     */
    public String getErrorHandlingPrompt(String error, String context) throws PromptProcessingException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("error", error);
        variables.put("context", context);
        return buildPrompt("common.错误处理提示词", variables);
    }

    /**
     * 获取结果验证提示词
     */
    public String getResultValidationPrompt(Object result) throws PromptProcessingException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("result", result);
        return buildPrompt("common.结果验证提示词", variables);
    }

    /**
     * 获取质量评估提示词
     */
    public String getQualityAssessmentPrompt(Object workContent) throws PromptProcessingException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("workContent", workContent);
        return buildPrompt("common.质量评估提示词", variables);
    }

    /**
     * 获取优化建议提示词
     */
    public String getOptimizationSuggestionPrompt(Object currentContent, String optimizationGoal) throws PromptProcessingException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("currentContent", currentContent);
        variables.put("optimizationGoal", optimizationGoal);
        return buildPrompt("common.优化建议提示词", variables);
    }

    /**
     * 获取总结生成提示词
     */
    public String getSummaryGenerationPrompt(Object originalContent) throws PromptProcessingException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("originalContent", originalContent);
        return buildPrompt("common.总结生成提示词", variables);
    }

    /**
     * 获取格式转换提示词
     */
    public String getFormatConversionPrompt(Object originalContent, String targetFormat) throws PromptProcessingException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("originalContent", originalContent);
        variables.put("targetFormat", targetFormat);
        return buildPrompt("common.格式转换提示词", variables);
    }

    /**
     * 重新加载提示词
     */
    public void reloadPrompts() throws PromptProcessingException {
        initialized = false;
        promptTemplates.clear();
        initialize();
        log.info("提示词重新加载完成");
    }

    /**
     * 获取所有提示词名称
     */
    public Map<String, String> getAllPromptTemplates() throws PromptProcessingException {
        if (!initialized) {
            initialize();
        }
        return new HashMap<>(promptTemplates);
    }

    /**
     * 获取指定Agent的提示词
     */
    public Map<String, String> getAgentPrompts(String agentName) throws PromptProcessingException {
        if (!initialized) {
            initialize();
        }

        Map<String, String> agentPrompts = new HashMap<>();
        String prefix = agentName + ".";

        for (Map.Entry<String, String> entry : promptTemplates.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                String promptName = entry.getKey().substring(prefix.length());
                agentPrompts.put(promptName, entry.getValue());
            }
        }

        return agentPrompts;
    }
}
