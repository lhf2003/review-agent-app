package com.review.agent.common.exception;

/**
 * 枚举类
 *
 * @Author: lhf
 */
public enum ErrorCode {
    PARAM_ERROR(40000, "请求参数错误"),
    USER_NOT_FOUND(40002, "用户不存在"),
    USER_EXIST(40003, "用户已存在"),
    NOT_FOUND_ERROR(40004, "资源不存在"),
    NOT_FOUND_DIRECTORY(40004, "目录不存在"),

    USER_NOT_LOGIN(40100, "用户未登录"),
    DATA_ALREADY_EXISTS(40002, "数据已存在"),
    NULL_DATA(40004, "数据为空"),
    PASSWORD_ERROR(40015, "密码错误"),
    DATA_IN_USE(40003, "数据正在被使用"),
    AUTH_ERROR(40005, "权限不足"),
    SUCCESS(0, "操作成功"),
    ERROR(50000, "操作失败"),
    SYSTEM_ERROR(50001, "系统内部错误"),
    SEND_MAIL_ERROR(50001, "发送邮件失败"),

    PROMPT_PROCESSING_ERROR(5001, "提示词处理错误"),
    PROMPT_NOT_FOUND(4004, "提示词未找到"),
    PROMPT_TEMPLATE_ERROR(5002, "提示词模板错误"),


    // ========== AI模型相关错误 (1000-1099) ==========
    /**
     * LLM服务连接失败
     */
    LLM_CONNECTION_FAILED(1001, "LLM服务连接失败"),

    /**
     * LLM服务超时
     */
    LLM_TIMEOUT(1002, "LLM服务超时"),

    /**
     * LLM服务调用失败
     */
    LLM_SERVICE_ERROR(1003, "LLM服务调用失败"),

    // ========== 数据库相关错误 (6000-6999) ==========

    /**
     * 数据库操作失败
     */
    DATA_NOT_FOUND(6001, "数据不存在"),

    /**
     * 数据库超时
     */
    DATABASE_TIMEOUT(6002, "数据库超时"),

    // ========== Redis相关错误 (7000-7999) ==========
    /**
     * Redis连接失败
     */
    REDIS_CONNECTION_FAILED(7000, "Redis连接失败"),

    /**
     * Redis操作失败
     */
    REDIS_OPERATION_FAILED(7001, "Redis操作失败"),

    /**
     * Redis超时
     */
    REDIS_TIMEOUT(7002, "Redis超时"),

    // ========== 网络错误 (8000-8999) ==========
    /**
     * 网络连接失败
     */
    NETWORK_CONNECTION_FAILED(8000, "网络连接失败"),
    /**
     * 网络超时
     */
    NETWORK_TIMEOUT(8001, "网络超时");

    private int code;
    private String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}