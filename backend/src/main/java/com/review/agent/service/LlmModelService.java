package com.review.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.entity.dto.LlmModelDTO;
import com.review.agent.entity.pojo.UserLlmConfig;
import com.review.agent.repository.UserLlmConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class LlmModelService {
    private final UserLlmConfigRepository userLlmConfigRepository;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public Boolean connectLlmProvider(UserLlmConfig userLlmConfig) {
        try {
            List<LlmModelDTO> models = getLlmModels(userLlmConfig);
            return models != null && !models.isEmpty();
        } catch (Exception e) {
            log.error("Failed to connect to LLM provider: {}", userLlmConfig.getName(), e);
            return false;
        }
    }

    public List<LlmModelDTO> getLlmModels(UserLlmConfig userLlmConfig) {
        String providerName = userLlmConfig.getName().toLowerCase();
        String baseUrl = userLlmConfig.getUrl();
        String apiKey = userLlmConfig.getApiKey();


        if ("ollama".equals(providerName)) {
            return getOllamaModels(baseUrl);
        } else if ("deepseek".equals(providerName)) {
            return getOpenAICompatibleModels("https://api.deepseek.com", apiKey);
        } else if ("bailian".equals(providerName)) {
            // Use Bailian's OpenAI compatible endpoint
            return getOpenAICompatibleModels("https://dashscope.aliyuncs.com/compatible-mode/v1", apiKey);
        } else if ("openai".equals(providerName) || "glm".equals(providerName) || "gemini".equals(providerName)) {
             // Generic OpenAI compatible or specific implementation if needed. 
             // For now, assuming user only asked for deepseek, bailian, ollama first, but I can add generic support if URL is provided.
             if (StringUtils.hasText(baseUrl)) {
                 return getOpenAICompatibleModels(baseUrl, apiKey);
             }
        }
        
        return Collections.emptyList();
    }

    private List<LlmModelDTO> getOllamaModels(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "http://localhost:11434";
        }
        // Remove trailing slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        try {
            String response = restClientBuilder.build()
                    .get()
                    .uri(baseUrl + "/api/tags")
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode modelsNode = root.path("models");
            List<LlmModelDTO> models = new ArrayList<>();

            if (modelsNode.isArray()) {
                for (JsonNode node : modelsNode) {
                    String name = node.path("name").asText();
                    List<String> capabilities = new ArrayList<>();
                    
                    models.add(LlmModelDTO.builder()
                            .id(name)
                            .object("model")
                            .ownedBy("ollama")
                            .capabilities(capabilities)
                            .build());
                }
            }
            return models;
        } catch (Exception e) {
            log.error("Failed to fetch Ollama models", e);
            throw new RuntimeException("Failed to fetch Ollama models: " + e.getMessage());
        }
    }

    private List<LlmModelDTO> getOpenAICompatibleModels(String baseUrl, String apiKey) {
         if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        try {
            String response = restClientBuilder.build()
                    .get()
                    .uri(baseUrl + "/models")
                    .header("Authorization", "Bearer " + apiKey)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.path("data");
            List<LlmModelDTO> models = new ArrayList<>();

            if (dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    String id = node.path("id").asText();
                    String object = node.path("object").asText("model");
                    String ownedBy = node.path("owned_by").asText();
                    
                    List<String> capabilities = new ArrayList<>();
                    if (id.toLowerCase().contains("coder") || id.toLowerCase().contains("tool")) {
                        capabilities.add("tools");
                    }
                    // DeepSeek specific
                    if (id.toLowerCase().contains("reasoner")) {
                        capabilities.add("reasoning");
                    }

                    models.add(LlmModelDTO.builder()
                            .id(id)
                            .object(object)
                            .ownedBy(ownedBy)
                            .capabilities(capabilities)
                            .build());
                }
            }
            return models;
        } catch (Exception e) {
            log.error("Failed to fetch OpenAI compatible models from {}", baseUrl, e);
            throw new RuntimeException("Failed to fetch models: " + e.getMessage());
        }
    }
}
