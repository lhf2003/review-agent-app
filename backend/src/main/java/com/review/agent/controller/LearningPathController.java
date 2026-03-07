package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.vo.LearningPathVO;
import com.review.agent.service.LearningPathService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学习路径控制器
 * 提供基于标签关系的个性化学习路径推荐
 */
@RestController
@RequestMapping("/learning-path")
public class LearningPathController {

    @Resource
    private LearningPathService learningPathService;

    /**
     * 获取学习路径推荐
     * @param limit 限制数量（默认5条）
     * @return 学习路径列表
     */
    @GetMapping("/recommendations")
    public BaseResponse<List<LearningPathVO>> getRecommendations(
            @RequestParam(defaultValue = "5") int limit) {
        return ResultUtil.success(learningPathService.getLearningPathRecommendations(limit));
    }

    /**
     * 获取特定标签的学习路径
     * @param tagId 标签ID
     * @return 学习路径详情
     */
    @GetMapping("/tag/{tagId}")
    public BaseResponse<LearningPathVO> getLearningPathForTag(@PathVariable Long tagId) {
        return ResultUtil.success(learningPathService.getLearningPathForTag(tagId));
    }

    /**
     * 获取学习进度概览
     * @return 学习进度统计
     */
    @GetMapping("/progress-overview")
    public BaseResponse<Map<String, Object>> getLearningProgressOverview() {
        return ResultUtil.success(learningPathService.getLearningProgressOverview());
    }
}
