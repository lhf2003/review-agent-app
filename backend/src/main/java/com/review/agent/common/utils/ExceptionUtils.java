package com.review.agent.common.utils;

import com.review.agent.common.exception.BusinessException;
import com.review.agent.common.exception.ErrorCode;
import com.review.agent.common.exception.ExternalServiceException;
import com.review.agent.common.exception.SystemException;
import lombok.extern.slf4j.Slf4j;

/**
 * 异常处理工具类
 */
@Slf4j
public class ExceptionUtils {
    
    /**
     * 抛出业务异常
     */
    public static void throwBusinessException(ErrorCode errorCode) {
        throw new BusinessException(errorCode);
    }
    
    /**
     * 抛出业务异常（自定义消息）
     */
    public static void throwBusinessException(ErrorCode errorCode, String customMessage) {
        throw new BusinessException(errorCode, customMessage);
    }

    /**
     * 密码错误时抛出异常
     */
    public static void throwPasswordError() {
        throw new BusinessException(ErrorCode.PASSWORD_ERROR, "密码错误");
    }

    /**
     * 密码错误时抛出异常
     */
    public static void throwParamError(String message) {
        throw new BusinessException(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 数据已存在时抛出异常
     */
    public static void throwDataAlreadyExists(String dataName) {
        throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, dataName + "已存在");
    }

    /**
     * 删除的数据正在使用时抛出异常
     */
    public static void throwDataInUse(String dataName) {
        throw new BusinessException(ErrorCode.DATA_IN_USE, dataName + "正在使用，无法删除");
    }

    /**
     * 数据库不存在该数据时抛出异常
     */
    public static void throwDataNotFound(String message) {
        throw new BusinessException(ErrorCode.DATA_NOT_FOUND, message);
    }

    /**
     * LLM服务连接失败时抛出异常
     */
    public static void throwLLMConnectionFailed(String message) {
        throw new ExternalServiceException(ErrorCode.LLM_CONNECTION_FAILED, "LLM", message);
    }

    /**
     * LLM服务超时时抛出异常
     */
    public static void throwLLMTimeout(String message) {
        throw new ExternalServiceException(ErrorCode.LLM_TIMEOUT, "LLM", message);
    }

    /**
     * LLM服务调用失败时抛出异常
     */
    public static void throwLLMError(String message) {
        throw new ExternalServiceException(ErrorCode.LLM_SERVICE_ERROR, "LLM", message);
    }

    /**
     * 网络连接失败时抛出异常
     */
    public static void throwNetworkError(String message) {
        throw new BusinessException(ErrorCode.NETWORK_CONNECTION_FAILED, message);
    }
    
    /**
     * 网络超时时抛出异常
     */
    public static void throwNetworkTimeout(String message) {
        throw new BusinessException(ErrorCode.NETWORK_TIMEOUT, message);
    }
    
    /**
     * 安全地执行操作，捕获异常并转换为业务异常
     */
    public static <T> T safeExecute(Operation<T> operation, ErrorCode errorCode) {
        try {
            return operation.execute();
        } catch (Exception e) {
            log.error("操作执行失败: {}", e.getMessage(), e);
            throw new BusinessException(errorCode, e.getMessage(), e);
        }
    }
    
    /**
     * 安全地执行操作，捕获异常并转换为系统异常
     */
    public static <T> T safeExecuteSystem(Operation<T> operation, ErrorCode errorCode) {
        try {
            return operation.execute();
        } catch (Exception e) {
            log.error("系统操作执行失败: {}", e.getMessage(), e);
            throw new SystemException(errorCode, e.getMessage(), e);
        }
    }
    
    /**
     * 操作接口
     */
    @FunctionalInterface
    public interface Operation<T> {
        T execute() throws Exception;
    }
}
