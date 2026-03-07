package com.review.agent.entity.vo;

import lombok.Data;

/**
 * 范式图谱节点VO（用于知识图谱层）
 */
@Data
public class ParadigmNodeVO {
    private String id;
    private String label;
    private String type; // PARADIGM|STEP|DECISION
    private String paradigmCode;
    private String description;
    private Integer size;
    private String color;
}
