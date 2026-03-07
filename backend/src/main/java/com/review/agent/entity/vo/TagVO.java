package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标签VO（新）
 */
@Data
public class TagVO {
    private Long id;
    private String name;
    private Long dimensionId;
    private String dimensionName;
    private Long parentId;
    private String parentName;
    private Integer level;
    private String path;

    // 思维范式专用字段
    private String paradigmCode;
    private String description;
    private String whenToUse;
    private String example;

    // 是否为系统内置标签
    private Boolean isSystemTag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    // 子标签列表
    private List<TagVO> children;

    // 关联的思维范式标签（用于技术标签关联的思维范式）
    private List<TagVO> relatedParadigms;
}
