package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.vo.ParadigmFlowchartVO;
import com.review.agent.entity.vo.ParadigmGraphLayerVO;
import com.review.agent.entity.vo.ParadigmVisualizationVO;
import com.review.agent.service.ParadigmVisualizationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 思维范式可视化接口
 */
@Slf4j
@RestController
@RequestMapping("/paradigm-visualization")
public class ParadigmVisualizationController {

    @Resource
    private ParadigmVisualizationService visualizationService;

    @Resource
    private SecurityUtils securityUtils;

    /**
     * 获取分析结果的思维范式可视化列表
     */
    @GetMapping("/result/{analysisResultId}")
    public BaseResponse<List<ParadigmVisualizationVO>> getVisualizationsByResult(
            @PathVariable Long analysisResultId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(visualizationService.getVisualizationsByResult(analysisResultId, userId));
    }

    /**
     * 获取流程图详情
     */
    @GetMapping("/flowchart/{flowchartId}")
    public BaseResponse<ParadigmFlowchartVO> getFlowchartDetail(
            @PathVariable Long flowchartId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(visualizationService.getFlowchartDetail(flowchartId, userId));
    }

    /**
     * 获取知识图谱范式层数据
     */
    @GetMapping("/graph-layer")
    public BaseResponse<ParadigmGraphLayerVO> getGraphLayer(
            @RequestParam(required = false) List<String> paradigmCodes) {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(visualizationService.getGraphLayer(userId, paradigmCodes));
    }

    /**
     * 重新生成可视化
     */
    @PostMapping("/regenerate/{analysisResultId}")
    public BaseResponse<Void> regenerateVisualization(
            @PathVariable Long analysisResultId) {
        Long userId = securityUtils.getCurrentUserId();
        visualizationService.regenerateVisualization(analysisResultId, userId);
        return ResultUtil.success();
    }
}
