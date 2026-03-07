package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标签维度VO
 */
@Data
public class TagDimensionVO {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String icon;
    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    // 该维度下的标签列表
    private List<TagVO> tags;
}
