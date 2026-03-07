package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 思维范式流程图VO
 */
@Data
public class ParadigmFlowchartVO {
    private Long id;
    private String paradigmCode;
    private String paradigmName;
    private Long analysisResultId;
    private String title;
    private String description;
    private List<FlowNodeVO> nodes;
    private List<FlowEdgeVO> edges;
    private Map<String, Object> layout;
    private String paradigmType;
    private Integer nodeCount;
    private Integer complexityScore;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;
}
