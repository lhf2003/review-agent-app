package com.review.agent.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> handleException(RuntimeException ex) {
        log.error(ex.getMessage(), ex);
        return new BaseResponse<>(ErrorCode.ERROR.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public BaseResponse<?> handleResourceAccessException(ResourceAccessException ex) {
        log.error(ex.getMessage(), ex);
        return new BaseResponse<>(ErrorCode.NOT_FOUND_ERROR.getCode(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<?> handleException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return new BaseResponse<>(ErrorCode.ERROR.getCode(), ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error(ex.getMessage(), ex);
        return new BaseResponse<>(ErrorCode.NULL_DATA.getCode(), ex.getMessage());
    }

    /**
     * 处理元素不存在异常
     * @param ex 元素不存在异常
     * @return 错误响应
     */
    @ExceptionHandler(NoSuchElementException.class)
    public BaseResponse<?> handleNoSuchElementException(NoSuchElementException ex) {
        log.error(ex.getMessage(), ex);
        return new BaseResponse<>(ErrorCode.NULL_DATA.getCode(), ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public BaseResponse<?> handleIOException(IOException ex) {
        log.error(ex.getMessage(), ex);
        return new BaseResponse<>(ErrorCode.ERROR.getCode(), ex.getMessage());
    }

}