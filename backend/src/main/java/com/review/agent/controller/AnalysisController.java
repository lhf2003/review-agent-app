package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.request.AnalysisRequest;
import com.review.agent.service.AnalysisService;
import com.review.agent.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分析接口
 */
@Slf4j
@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    @Resource
    private AnalysisService analysisService;

    /**
     * 开始分析
     */
    @PostMapping("/start")
    public BaseResponse<?> startAnalysis(@RequestBody AnalysisRequest analysisRequest) {
        analysisService.startAnalysis(analysisRequest);
        return ResultUtil.success("analysis started");
    }
}