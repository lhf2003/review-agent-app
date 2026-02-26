package com.review.agent.entity.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 提交单个答案请求
 */
@Getter
@Setter
public class SubmitAnswerRequest {

    /**
     * 题目ID
     */
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    /**
     * 用户答案
     */
    @NotBlank(message = "答案不能为空")
    private String userAnswer;

    /**
     * 是否为复习模式提交
     */
    private Boolean reviewMode = false;
}
