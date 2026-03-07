package com.review.agent.entity.dto;

import lombok.Data;

import java.util.Map;

@Data
public class NodeExecuteDto {
    private String problemStatement;
    private String solution;

    // 技术标签（旧）
    private Long tagId;
    private String subTagId;
    private String subTagName;
    private String recommends;

    // 思维范式（新）
    private String thinkingParadigmIds;      // 思维范式标签ID列表，逗号分隔
    private String thinkingParadigms;        // 思维范式名称列表，逗号分隔
    private String explorationPath;          // 探索路径描述
    private String thinkingQuality;          // 思考质量（high/medium/low/unknown）
    private Map<String, Object> paradigmDetails; // 范式详细信息的JSON对象

    // 多维度标签结果（新）
    private MultiDimensionTagResult multiDimensionResult; // 完整的维度分类结果

    private Integer status;
}