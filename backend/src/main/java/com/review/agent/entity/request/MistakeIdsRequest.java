package com.review.agent.entity.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 错题ID列表请求（用于批量标记掌握/删除）
 */
@Getter
@Setter
public class MistakeIdsRequest {

    /**
     * 错题ID列表
     */
    @NotEmpty(message = "错题ID列表不能为空")
    private List<Long> questionIds;
}
