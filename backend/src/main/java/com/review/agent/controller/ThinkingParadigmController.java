package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.Tag;
import com.review.agent.entity.pojo.TagDimension;
import com.review.agent.entity.vo.TagDimensionVO;
import com.review.agent.entity.vo.TagVO;
import com.review.agent.entity.vo.ThinkingParadigmVO;
import com.review.agent.service.ThinkingParadigmService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 思维范式接口
 */
@RestController
@RequestMapping("/thinking-paradigm")
public class ThinkingParadigmController {

    @Resource
    private ThinkingParadigmService thinkingParadigmService;

    @Resource
    private SecurityUtils securityUtils;

    /**
     * 获取所有标签维度
     */
    @GetMapping("/dimensions")
    public BaseResponse<List<TagDimensionVO>> getDimensions() {
        return ResultUtil.success(thinkingParadigmService.getAllDimensions());
    }

    /**
     * 获取思维范式标签列表
     */
    @GetMapping("/list")
    public BaseResponse<List<TagVO>> getThinkingParadigms() {
        return ResultUtil.success(thinkingParadigmService.getAllThinkingParadigms());
    }

    /**
     * 获取分析结果的思维范式
     */
    @GetMapping("/result/{analysisResultId}")
    public BaseResponse<List<ThinkingParadigmVO>> getParadigmsByResult(
            @PathVariable Long analysisResultId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(thinkingParadigmService.getParadigmsByAnalysisResult(userId, analysisResultId));
    }

    /**
     * 获取学习路径推荐
     * 基于思维范式依赖关系推荐学习顺序
     */
    @GetMapping("/learning-path")
    public BaseResponse<List<TagVO>> getLearningPath(
            @RequestParam(required = false) Long targetParadigmId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(thinkingParadigmService.getLearningPath(userId, targetParadigmId));
    }

    /**
     * 获取相似思维范式的知识
     */
    @GetMapping("/similar/{paradigmId}")
    public BaseResponse<List<TagVO>> getSimilarParadigms(@PathVariable Long paradigmId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(thinkingParadigmService.getSimilarParadigms(userId, paradigmId));
    }

    /**
     * 获取用户掌握的思维范式统计
     */
    @GetMapping("/stats")
    public BaseResponse getParadigmStats() {
        Long userId = securityUtils.getCurrentUserId();
        return ResultUtil.success(thinkingParadigmService.getUserParadigmStats(userId));
    }
}
