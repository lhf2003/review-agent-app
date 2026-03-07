package com.review.agent.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 简化版知识图谱 VO
 * 用于知识图谱可视化展示
 */
@Data
public class SimpleKnowledgeGraphVO {

    /**
     * 节点列表
     */
    private List<SimpleNodeVO> nodes;

    /**
     * 边列表
     */
    private List<SimpleEdgeVO> edges;
}
