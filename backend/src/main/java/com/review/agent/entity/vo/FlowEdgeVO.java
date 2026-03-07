package com.review.agent.entity.vo;

import lombok.Data;

/**
 * 流程图边VO
 */
@Data
public class FlowEdgeVO {
    private String source;
    private String target;
    private String label;
}
