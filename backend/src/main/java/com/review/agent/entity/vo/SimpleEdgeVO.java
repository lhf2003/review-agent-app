package com.review.agent.entity.vo;

import lombok.Data;

/**
 * 知识图谱边 VO
 */
@Data
public class SimpleEdgeVO {

    /**
     * 源节点 ID
     */
    private String source;

    /**
     * 目标节点 ID
     */
    private String target;
}
