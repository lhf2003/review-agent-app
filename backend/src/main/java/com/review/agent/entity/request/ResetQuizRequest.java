package com.review.agent.entity.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 重置测验请求
 */
@Getter
@Setter
public class ResetQuizRequest {

    /**
     * 测验ID
     */
    @NotNull(message = "测验ID不能为空")
    private Long quizId;
}
