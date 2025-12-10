package com.review.agent.entity.dto;

import lombok.Data;

@Data
public class NodeExecuteDto {
    private Long userId;
    private Long fileId;
    private String problemStatement;
    private String solution;
    private Integer sessionStart;
    private Integer sessionEnd;
    private String sessionContent;
    private Long tagId;
    private String subTagId;
    private String subTagName;
    private String recommends;
    private Integer status;
}