package com.review.agent.entity.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalTime;

/**
 * 用户配置更新请求
 */
@Data
public class UserConfigUpdateRequest {
    private String scanDirectory;

    private Boolean autoScanEnabled;

    private Integer scanIntervalSeconds;

    private String llmProvider;

    private String tagLlmProvider;

    private String sessionLlmProvider;

    private String openaiApiKeyEncrypted;

    /**
     * 是否启用每日分析
     */
    private Boolean dailyEnabled;

    /**
     * 日报的执行时间
     */
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    private LocalTime dailyAnalysisTime;

    /**
     * 是否启用每周分析
     */
    private Boolean weeklyEnabled;

    /**
     * 周报分析的星期几
     */
    private Integer weeklyAnalysisDay;
    /**
     * 周报的分析时间
     */
    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    private LocalTime weeklyAnalysisTime;
}