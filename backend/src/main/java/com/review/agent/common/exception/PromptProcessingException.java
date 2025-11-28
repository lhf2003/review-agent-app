package com.review.agent.common.exception;


public class PromptProcessingException extends RuntimeException {

    private final Integer code;
    private final String message;

    public PromptProcessingException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public PromptProcessingException(String message) {
        this(ErrorCode.PROMPT_PROCESSING_ERROR.getCode(), message);
    }

    public PromptProcessingException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message);
    }

    public PromptProcessingException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage());
    }
}
