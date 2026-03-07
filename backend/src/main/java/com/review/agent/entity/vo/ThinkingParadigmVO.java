package com.review.agent.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 思维范式VO
 * 用于展示分析结果中的思维范式信息
 */
@Data
public class ThinkingParadigmVO {
    private Long id;
    private String code;
    private String name;
    private Integer confidence;
    private String application;
    private String keyInsight;
    private String description;
    private String whenToUse;
    private String example;
}
