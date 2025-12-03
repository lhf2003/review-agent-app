package com.review.agent.common.exception;

/**
 * 外部服务异常类
 * 用于处理外部服务调用相关的异常
 */
public class ExternalServiceException extends RuntimeException {
    
    private final Integer code;
    private final String message;
    private final String serviceName;
    
    public ExternalServiceException(ErrorCode errorCode, String serviceName) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(ErrorCode errorCode, String serviceName, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
        this.message = customMessage;
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(Integer code, String message, String serviceName) {
        super(message);
        this.code = code;
        this.message = message;
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(ErrorCode errorCode, String serviceName, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.serviceName = serviceName;
    }
    
    public ExternalServiceException(ErrorCode errorCode, String serviceName, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.code = errorCode.getCode();
        this.message = customMessage;
        this.serviceName = serviceName;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    @Override
    public String toString() {
        return "ExternalServiceException{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }
}
