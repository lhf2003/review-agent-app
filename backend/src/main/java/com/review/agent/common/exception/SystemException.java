package com.review.agent.common.exception;

/**
 * 系统异常类
 * 用于处理系统级别的异常
 */
public class SystemException extends RuntimeException {
    
    private final Integer code;
    private final String message;
    
    public SystemException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
    
    public SystemException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.message = customMessage;
    }
    
    public SystemException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
    
    public SystemException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.code = errorCode.getCode();
        this.message = customMessage;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    @Override
    public String toString() {
        return "SystemException{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
