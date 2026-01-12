package com.review.agent.common.constant;

/**
 * 公共常量
 */
public class CommonConstant {
    /**
     * 文件处理状态-未处理
     */
    public static final int FILE_PROCESS_STATUS_NOT_PROCESSED = 0;
    /**
     * 文件处理状态-处理中
     */
    public static final int FILE_PROCESS_STATUS_PROCESSING = 1;
    /**
     * 文件处理状态-已处理
     */
    public static final int FILE_PROCESS_STATUS_PROCESSED = 2;
    /**
     * 文件处理状态-有更新
     */
    public static final int FILE_PROCESS_STATUS_UPDATE = 3;
    /**
     * 文件处理状态-处理错误
     */
    public static final int FILE_PROCESS_STATUS_ERROR = 4;

    // region AI分析状态

    /**
     * 分析状态-已处理
     */
    public static final int ANALYSIS_STATUS_PROCESSED = 1;
    /**
     * 分析状态-处理错误
     */
    public static final int ANALYSIS_STATUS_ERROR = 0;

    // endregion

    /**
     * 报告类型-日报
     */
    public static final int DAILY_REPORT = 1;

     /**
     * 报告类型-周报
     */
    public static final int WEEKLY_REPORT = 2;

    // region 数据来源

    /**
     * 数据来源-本地
     */
    public static final int DATA_SOURCE_LOCAL = 0;

    /**
     * 数据来源-Gemini
     */
    public static final int DATA_SOURCE_GEMINI = 1;

    /**
     * 数据来源-ChatGPT
     */
    public static final int DATA_SOURCE_CHATGPT = 2;

    // endregion

}