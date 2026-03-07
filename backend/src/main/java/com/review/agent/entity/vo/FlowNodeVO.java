package com.review.agent.entity.vo;

import lombok.Data;

/**
 * 流程图节点VO
 */
@Data
public class FlowNodeVO {
    private String id;
    private String label;
    private String type; // START|PROCESS|DECISION|END
    private String description;
    private String codeSnippet;
    private Integer x;
    private Integer y;
}
