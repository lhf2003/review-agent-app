package com.review.agent.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 会话跟踪VO
 */
@Data
public class SessionTraceVo {
    private String content;
    private List<AnalysisResultInfo> analysisResultInfoList;

    @Data
    public static class AnalysisResultInfo {
        private Long analysisResultId;
        private String problemStatement;
        private String solution;
        private String originContent;
        private Integer startIndex;
        private Integer endIndex;
    }
}