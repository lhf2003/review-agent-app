package com.review.agent.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 思维范式图谱层VO
 */
@Data
public class ParadigmGraphLayerVO {
    private List<ParadigmNodeVO> nodes;
    private List<ParadigmEdgeVO> edges;
}
