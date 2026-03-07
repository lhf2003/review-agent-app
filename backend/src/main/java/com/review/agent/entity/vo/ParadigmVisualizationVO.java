package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 思维范式可视化列表项VO
 */
@Data
public class ParadigmVisualizationVO {
    private Long id;
    private String paradigmCode;
    private String paradigmName;
    private String paradigmType;
    private String title;
    private String description;
    private Integer nodeCount;
    private Integer complexityScore;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;
}
