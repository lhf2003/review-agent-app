package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分析结果详情VO（包含思维范式信息）
 */
@Data
public class AnalysisResultDetailVO {
    private Long id;
    private Long fileId;
    private String fileName;
    private String problemStatement;
    private String solution;
    private String sessionContent;

    // 技术标签
    private String mainTagName;
    private List<String> subTagNameList;
    private List<String> recommendTagList;

    // 思维范式信息（新）
    private List<ThinkingParadigmVO> thinkingParadigms;
    private String explorationPath;
    private String thinkingQuality;
    private Map<String, Object> paradigmDetails;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
