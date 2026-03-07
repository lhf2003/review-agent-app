package com.review.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.dto.NodeExecuteDto;
import com.review.agent.entity.pojo.ParadigmFlowchart;
import com.review.agent.entity.pojo.Tag;
import com.review.agent.entity.vo.*;
import com.review.agent.graph.utils.NodeRetryHelper;
import com.review.agent.repository.ParadigmFlowchartRepository;
import com.review.agent.repository.TagRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 思维范式可视化服务
 */
@Slf4j
@Service
public class ParadigmVisualizationService {

    @Resource
    private ParadigmFlowchartRepository flowchartRepository;

    @Resource
    private TagRepository tagRepository;

    @Resource
    private PromptService promptService;

    @Resource
    private SecurityUtils securityUtils;

    @Resource(name = "classifyChatClient")
    private ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 在ThinkingParadigmNode分析完成后调用，提取并保存流程图
     */
    @Transactional(rollbackFor = Exception.class)
    public void extractAndSaveVisualizations(Long analysisResultId, Long userId,
            List<NodeExecuteDto> nodeResults) {
        log.info("开始提取思维范式可视化: analysisResultId={}, userId={}", analysisResultId, userId);

        // 删除旧的流程图
        List<ParadigmFlowchart> oldFlowcharts = flowchartRepository.findByAnalysisResultId(analysisResultId);
        flowchartRepository.deleteAll(oldFlowcharts);

        int savedCount = 0;

        for (NodeExecuteDto nodeResult : nodeResults) {
            // 检查是否有范式详细信息
            Map<String, Object> paradigmDetails = nodeResult.getParadigmDetails();
            if (paradigmDetails == null || !paradigmDetails.containsKey("paradigms")) {
                continue;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> paradigms = (List<Map<String, Object>>) paradigmDetails.get("paradigms");
            if (paradigms == null || paradigms.isEmpty()) {
                continue;
            }

            // 为每个识别的范式提取流程图
            for (Map<String, Object> paradigm : paradigms) {
                String paradigmCode = (String) paradigm.get("code");
                if (paradigmCode == null || paradigmCode.isEmpty()) {
                    continue;
                }

                try {
                    ParadigmFlowchart flowchart = extractFlowchartWithAI(
                            paradigmCode, nodeResult.getSessionContent(), nodeResult);

                    if (flowchart != null) {
                        flowchart.setAnalysisResultId(analysisResultId);
                        flowchart.setUserId(userId);
                        flowchart.setParadigmCode(paradigmCode);
                        flowchart.setParadigmName((String) paradigm.get("name"));

                        flowchartRepository.save(flowchart);
                        savedCount++;
                    }
                } catch (Exception e) {
                    log.error("提取流程图失败: paradigmCode={}, error={}", paradigmCode, e.getMessage());
                }
            }
        }

        log.info("思维范式可视化提取完成: analysisResultId={}, savedCount={}", analysisResultId, savedCount);
    }

    /**
     * 使用AI从对话内容中提取流程图结构
     */
    private ParadigmFlowchart extractFlowchartWithAI(String paradigmCode,
            String sessionContent, NodeExecuteDto nodeResult) {

        // 获取范式定义
        Tag paradigmTag = tagRepository.findByParadigmCode(paradigmCode).orElse(null);
        if (paradigmTag == null) {
            log.warn("未找到范式标签: {}", paradigmCode);
            return null;
        }

        String paradigmName = paradigmTag.getName();
        String paradigmDefinition = buildParadigmDefinition(paradigmTag);

        // 构建提示词
        String prompt = buildFlowchartExtractionPrompt(paradigmCode, paradigmName, paradigmDefinition, sessionContent);

        try {
            // 调用AI提取流程图
            FlowchartExtractionResult result = NodeRetryHelper.builder()
                    .operation("FlowchartExtraction[" + paradigmCode + "]")
                    .chatClient(chatClient)
                    .systemPrompt("你是一个思维范式可视化专家。请从对话中提取思维范式的流程图结构。")
                    .userPrompt(prompt)
                    .execute(FlowchartExtractionResult.class, this::createDefaultFlowchartResult);

            if (result == null || result.nodes() == null || result.nodes().isEmpty()) {
                return null;
            }

            // 转换为实体
            ParadigmFlowchart flowchart = new ParadigmFlowchart();
            flowchart.setTitle(result.title());
            flowchart.setDescription(result.description());
            flowchart.setParadigmType(result.paradigmType() != null ? result.paradigmType() : "FLOW_CHART");
            flowchart.setNodeCount(result.nodes().size());
            flowchart.setComplexityScore(calculateComplexityScore(result));

            // 序列化JSON字段
            flowchart.setNodes(objectMapper.writeValueAsString(result.nodes()));
            flowchart.setEdges(objectMapper.writeValueAsString(result.edges()));
            if (result.layout() != null) {
                flowchart.setLayout(objectMapper.writeValueAsString(result.layout()));
            }

            return flowchart;

        } catch (Exception e) {
            log.error("AI提取流程图失败: paradigmCode={}, error={}", paradigmCode, e.getMessage());
            return null;
        }
    }

    /**
     * 构建范式定义说明
     */
    private String buildParadigmDefinition(Tag tag) {
        StringBuilder sb = new StringBuilder();
        sb.append(tag.getName());
        if (tag.getDescription() != null) {
            sb.append("：").append(tag.getDescription());
        }
        if (tag.getWhenToUse() != null) {
            sb.append("，适用场景：").append(tag.getWhenToUse());
        }
        return sb.toString();
    }

    /**
     * 构建流程图提取提示词
     */
    private String buildFlowchartExtractionPrompt(String paradigmCode, String paradigmName,
            String paradigmDefinition, String sessionContent) {
        return String.format("""
                请从以下对话中提取"%s"思维范式的流程图结构。

                范式定义：%s

                对话内容：
                %s

                请提取流程图结构，返回JSON格式：
                {
                  "title": "流程图标题",
                  "description": "流程图描述",
                  "paradigmType": "FLOW_CHART",
                  "nodes": [
                    {
                      "id": "node1",
                      "label": "节点标签",
                      "type": "START|PROCESS|DECISION|END",
                      "description": "节点详细描述",
                      "codeSnippet": "相关代码片段（如有）"
                    }
                  ],
                  "edges": [
                    {
                      "source": "node1",
                      "target": "node2",
                      "label": "边标签（如有）"
                    }
                  ],
                  "layout": {
                    "type": "dagre|mindmap|indented",
                    "direction": "TB|LR"
                  }
                }

                注意：
                1. 节点类型必须是 START、PROCESS、DECISION、END 之一
                2. 必须包含至少一个 START 和一个 END 节点
                3. 节点ID必须唯一
                4. 边必须连接存在的节点ID
                """,
                paradigmName, paradigmDefinition, sessionContent);
    }

    /**
     * 计算复杂度评分
     */
    private Integer calculateComplexityScore(FlowchartExtractionResult result) {
        if (result.nodes() == null) {
            return 0;
        }

        int score = 0;

        // 节点数量评分（最多40分）
        int nodeCount = result.nodes().size();
        score += Math.min(nodeCount * 5, 40);

        // 决策节点评分（最多30分）
        long decisionCount = result.nodes().stream()
                .filter(n -> "DECISION".equals(n.getType()))
                .count();
        score += Math.min((int) decisionCount * 10, 30);

        // 边复杂度评分（最多30分）
        if (result.edges() != null) {
            int edgeCount = result.edges().size();
            score += Math.min(edgeCount * 3, 30);
        }

        return Math.min(score, 100);
    }

    /**
     * 创建默认流程图结果（降级策略）
     */
    private FlowchartExtractionResult createDefaultFlowchartResult() {
        List<FlowNodeVO> nodes = new ArrayList<>();
        List<FlowEdgeVO> edges = new ArrayList<>();

        FlowNodeVO startNode = new FlowNodeVO();
        startNode.setId("start");
        startNode.setLabel("开始");
        startNode.setType("START");
        nodes.add(startNode);

        FlowNodeVO endNode = new FlowNodeVO();
        endNode.setId("end");
        endNode.setLabel("结束");
        endNode.setType("END");
        nodes.add(endNode);

        FlowEdgeVO edge = new FlowEdgeVO();
        edge.setSource("start");
        edge.setTarget("end");
        edges.add(edge);

        return new FlowchartExtractionResult(
                "流程图",
                "未能提取详细流程图",
                "FLOW_CHART",
                nodes,
                edges,
                null
        );
    }

    /**
     * 获取知识图谱范式层数据（用于与现有知识图谱合并展示）
     */
    public ParadigmGraphLayerVO getGraphLayer(Long userId, List<String> paradigmCodes) {
        ParadigmGraphLayerVO layer = new ParadigmGraphLayerVO();
        List<ParadigmNodeVO> nodes = new ArrayList<>();
        List<ParadigmEdgeVO> edges = new ArrayList<>();

        // 查询用户的所有流程图
        List<ParadigmFlowchart> flowcharts;
        if (paradigmCodes != null && !paradigmCodes.isEmpty()) {
            flowcharts = new ArrayList<>();
            for (String code : paradigmCodes) {
                flowcharts.addAll(flowchartRepository.findByUserIdAndParadigmCode(userId, code));
            }
        } else {
            // 获取所有分析结果的流程图
            flowcharts = flowchartRepository.findByAnalysisResultId(0L);
            // 这里简化处理，实际应该查询所有
        }

        int colorIndex = 0;
        String[] colors = { "#5470c6", "#91cc75", "#fac858", "#ee6666", "#73c0de", "#3ba272" };

        for (ParadigmFlowchart flowchart : flowcharts) {
            String color = colors[colorIndex % colors.length];
            colorIndex++;

            // 添加范式根节点
            ParadigmNodeVO rootNode = new ParadigmNodeVO();
            rootNode.setId("paradigm_" + flowchart.getId());
            rootNode.setLabel(flowchart.getParadigmName());
            rootNode.setType("PARADIGM");
            rootNode.setParadigmCode(flowchart.getParadigmCode());
            rootNode.setDescription(flowchart.getDescription());
            rootNode.setSize(60);
            rootNode.setColor(color);
            nodes.add(rootNode);

            // 解析子节点
            try {
                if (flowchart.getNodes() != null) {
                    List<FlowNodeVO> flowNodes = objectMapper.readValue(flowchart.getNodes(),
                            new TypeReference<List<FlowNodeVO>>() {});

                    for (FlowNodeVO flowNode : flowNodes) {
                        ParadigmNodeVO node = new ParadigmNodeVO();
                        node.setId(flowchart.getId() + "_" + flowNode.getId());
                        node.setLabel(flowNode.getLabel());
                        node.setType(flowNode.getType());
                        node.setParadigmCode(flowchart.getParadigmCode());
                        node.setDescription(flowNode.getDescription());
                        node.setSize(getNodeSize(flowNode.getType()));
                        node.setColor(color);
                        nodes.add(node);

                        // 添加连接到根节点的边
                        ParadigmEdgeVO edge = new ParadigmEdgeVO();
                        edge.setSource("paradigm_" + flowchart.getId());
                        edge.setTarget(flowchart.getId() + "_" + flowNode.getId());
                        edge.setType("FLOW");
                        edges.add(edge);
                    }
                }

                // 解析边
                if (flowchart.getEdges() != null) {
                    List<FlowEdgeVO> flowEdges = objectMapper.readValue(flowchart.getEdges(),
                            new TypeReference<List<FlowEdgeVO>>() {});

                    for (FlowEdgeVO flowEdge : flowEdges) {
                        ParadigmEdgeVO edge = new ParadigmEdgeVO();
                        edge.setSource(flowchart.getId() + "_" + flowEdge.getSource());
                        edge.setTarget(flowchart.getId() + "_" + flowEdge.getTarget());
                        edge.setLabel(flowEdge.getLabel());
                        edge.setType("FLOW");
                        edges.add(edge);
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("解析流程图JSON失败: flowchartId={}", flowchart.getId(), e);
            }
        }

        layer.setNodes(nodes);
        layer.setEdges(edges);
        return layer;
    }

    /**
     * 根据节点类型获取大小
     */
    private Integer getNodeSize(String type) {
        return switch (type) {
            case "START", "END" -> 40;
            case "DECISION" -> 50;
            default -> 35;
        };
    }

    /**
     * 获取分析结果的所有可视化
     */
    public List<ParadigmVisualizationVO> getVisualizationsByResult(Long analysisResultId, Long userId) {
        List<ParadigmFlowchart> flowcharts = flowchartRepository.findByAnalysisResultId(analysisResultId);

        return flowcharts.stream()
                .filter(f -> f.getUserId().equals(userId))
                .map(this::convertToVisualizationVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为可视化VO
     */
    private ParadigmVisualizationVO convertToVisualizationVO(ParadigmFlowchart flowchart) {
        ParadigmVisualizationVO vo = new ParadigmVisualizationVO();
        BeanUtils.copyProperties(flowchart, vo);
        // 从Tag获取范式名称
        tagRepository.findByParadigmCode(flowchart.getParadigmCode())
                .ifPresent(tag -> vo.setParadigmName(tag.getName()));
        return vo;
    }

    /**
     * 获取流程图详情
     */
    public ParadigmFlowchartVO getFlowchartDetail(Long flowchartId, Long userId) {
        ParadigmFlowchart flowchart = flowchartRepository.findByIdAndUserId(flowchartId, userId)
                .orElseThrow(() -> new IllegalArgumentException("流程图不存在或无权访问"));

        ParadigmFlowchartVO vo = new ParadigmFlowchartVO();
        BeanUtils.copyProperties(flowchart, vo);

        // 从Tag获取范式名称
        tagRepository.findByParadigmCode(flowchart.getParadigmCode())
                .ifPresent(tag -> vo.setParadigmName(tag.getName()));

        // 解析JSON字段
        try {
            if (flowchart.getNodes() != null) {
                vo.setNodes(objectMapper.readValue(flowchart.getNodes(),
                        new TypeReference<List<FlowNodeVO>>() {}));
            }
            if (flowchart.getEdges() != null) {
                vo.setEdges(objectMapper.readValue(flowchart.getEdges(),
                        new TypeReference<List<FlowEdgeVO>>() {}));
            }
            if (flowchart.getLayout() != null) {
                vo.setLayout(objectMapper.readValue(flowchart.getLayout(),
                        new TypeReference<Map<String, Object>>() {}));
            }
        } catch (JsonProcessingException e) {
            log.error("解析流程图JSON失败: flowchartId={}", flowchartId, e);
            throw new RuntimeException("流程图数据解析失败");
        }

        return vo;
    }

    /**
     * 重新生成可视化
     */
    @Transactional(rollbackFor = Exception.class)
    public void regenerateVisualization(Long analysisResultId, Long userId) {
        // 删除旧数据，等待下次分析时重新生成
        List<ParadigmFlowchart> oldFlowcharts = flowchartRepository.findByAnalysisResultId(analysisResultId);
        // 验证权限
        oldFlowcharts = oldFlowcharts.stream()
                .filter(f -> f.getUserId().equals(userId))
                .collect(Collectors.toList());

        flowchartRepository.deleteAll(oldFlowcharts);
        log.info("已删除旧流程图，等待重新生成: analysisResultId={}", analysisResultId);
    }

    // ========== 内部记录类 ==========

    /**
     * 流程图提取结果记录
     */
    private record FlowchartExtractionResult(
            String title,
            String description,
            String paradigmType,
            List<FlowNodeVO> nodes,
            List<FlowEdgeVO> edges,
            Map<String, Object> layout) {
    }
}
