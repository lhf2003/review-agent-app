package com.review.agent.common;

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
     * 文件处理状态-处理错误
     */
    public static final int FILE_PROCESS_STATUS_ERROR = 3;

    // region AI分析状态

    /**
     * 分析状态-已处理
     */
    public static final int ANALYSIS_STATUS_PROCESSED = 1;
    /**
     * 分析状态-处理错误
     */
    public static final int ANALYSIS_STATUS_ERROR = 0;
}