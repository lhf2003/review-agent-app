package com.review.agent.entity.vo;

import lombok.Data;

/**
 * 范式图谱边VO（用于知识图谱层）
 */
@Data
public class ParadigmEdgeVO {
    private String source;
    private String target;
    private String label;
    private String type; // FLOW|DERIVATION
}
