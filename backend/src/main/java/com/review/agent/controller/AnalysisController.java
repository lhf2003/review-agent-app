package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.AnalysisResult;
import com.review.agent.entity.request.AnalysisRequest;
import com.review.agent.entity.request.AnalysisResultRequest;
import com.review.agent.entity.vo.AnalysisResultVo;
import com.review.agent.entity.vo.AnalysisTagVo;
import com.review.agent.service.AnalysisService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * 分页查询分析结果
     * @param pageable 分页参数
     * @param resultRequest 查询参数
     * @return 分析结果列表
     */
    @PostMapping("/page")
    public BaseResponse<List<AnalysisResultVo>> page(Pageable pageable, @RequestBody AnalysisResultRequest resultRequest) {
        return ResultUtil.success(analysisService.page(pageable, resultRequest));
    }

    /**
     * 获取分析标签列表
     * @param userId 用户ID
     * @return 分析标签列表
     */
    @GetMapping("/tag/list")
    public BaseResponse<List<AnalysisTagVo>> getTagList(@RequestParam("userId") Long userId) {
        return ResultUtil.success(analysisService.getTagList(userId));
    }

    /**
     * 开始分析
     * @param analysisRequest 分析请求
     * @return 分析结果
     */
    @PostMapping("/start")
    public BaseResponse<?> startAnalysis(@RequestBody AnalysisRequest analysisRequest) {
        analysisService.startAnalysis(analysisRequest);
        return ResultUtil.success("analysis success!");
    }

    /**
     * 获取指定会话的分析结果
     * @param userId 用户ID
     * @param dataId 数据ID
     * @return 分析结果
     */
    @GetMapping("/result")
    public BaseResponse<AnalysisResult> getAnalysisResult(@RequestParam("userId") Long userId, @RequestParam("dataId") Long dataId, @RequestParam("analysisId") Long analysisId) {
        return ResultUtil.success(analysisService.getAnalysisResult(userId, dataId, analysisId));
    }
}