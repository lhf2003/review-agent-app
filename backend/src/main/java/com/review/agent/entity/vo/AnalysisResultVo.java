package com.review.agent.entity.vo;

import lombok.Data;

import java.util.List;

@Data
public class AnalysisResultVo {
    private Long id;
    private Long fileId;
    private String vectorId;
    private String problemStatement;
    private String mainTagName;
    private List<String> subTagNameList;
}