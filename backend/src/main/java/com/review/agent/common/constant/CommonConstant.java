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

    public static final int DAILY_REPORT = 1;
    public static final int WEEKLY_REPORT = 2;

}