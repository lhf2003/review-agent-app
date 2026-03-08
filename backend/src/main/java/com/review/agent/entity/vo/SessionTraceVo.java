package com.review.agent.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 会话跟踪VO
 */
@Data
public class SessionTraceVo {
    private String content;
    private Long analysisResultId;
    private String problemStatement;
    private String solution;
}