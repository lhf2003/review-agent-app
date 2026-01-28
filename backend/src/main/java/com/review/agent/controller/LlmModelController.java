package com.review.agent.controller;

import com.review.agent.common.enums.LlmProvider;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.dto.LlmModelDTO;
import com.review.agent.entity.pojo.UserLlmConfig;
import com.review.agent.service.LlmModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/llm/model")
public class LlmModelController {
    private final LlmModelService llmModelService;

    /**
     * 获取模型提供商提供的模型列表
     */
    @PostMapping("/list")
    public BaseResponse<List<LlmModelDTO>> getLlmModels(@RequestBody UserLlmConfig userLlmConfig) {
        return ResultUtil.success(llmModelService.getLlmModels(userLlmConfig));
    }

    /**
     * 连接模型提供商：测试 APIKEY 是否正确
     */
    @PostMapping("/connect")
    public BaseResponse<Boolean> isConnected(@RequestBody UserLlmConfig userLlmConfig) {
        return ResultUtil.success(llmModelService.connectLlmProvider(userLlmConfig));
    }
}
