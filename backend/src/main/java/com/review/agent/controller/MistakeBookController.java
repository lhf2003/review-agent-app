package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.request.MistakeIdsRequest;
import com.review.agent.entity.vo.MistakeVo;
import com.review.agent.entity.vo.ReviewRecommendationVO;
import com.review.agent.service.MistakeBookService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 错题本控制器
 * 提供错题查询、统计、标记掌握、删除等功能
 */
@Slf4j
@RestController
@RequestMapping("/mistake-book")
public class MistakeBookController {

    @Resource
    private MistakeBookService mistakeBookService;

    @Resource
    private SecurityUtils securityUtils;

    /**
     * 获取错题列表
     *
     * @param filter 筛选条件：all(全部), unmastered(未掌握), mastered(已掌握)，默认为 all
     * @return 错题列表
     */
    @GetMapping("/list")
    public BaseResponse<List<MistakeVo>> getMistakeList(
            @RequestParam(defaultValue = "all") String filter) {
        try {
            List<MistakeVo> list = mistakeBookService.getMistakeList(filter);
            return ResultUtil.success(list);
        } catch (Exception e) {
            log.error("获取错题列表失败", e);
            return ResultUtil.error("获取错题列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取错题统计信息
     *
     * @return 统计信息：{total: 总数, unmastered: 未掌握, mastered: 已掌握}
     */
    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> getMistakeStats() {
        try {
            Map<String, Object> stats = mistakeBookService.getMistakeStats();
            return ResultUtil.success(stats);
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ResultUtil.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 批量标记错题为已掌握
     *
     * @param request 请求体 {mistakeIds: [1, 2, 3]}
     * @return 成功标记的数量
     */
    @PostMapping("/mark-mastered")
    public BaseResponse<Integer> markAsMastered(@Valid @RequestBody MistakeIdsRequest request) {
        try {
            int count = mistakeBookService.batchMarkMastered(request.getMistakeIds());
            return ResultUtil.success(count);
        } catch (Exception e) {
            log.error("标记已掌握失败", e);
            return ResultUtil.error("标记已掌握失败: " + e.getMessage());
        }
    }

    /**
     * 批量删除错题
     *
     * @param request 请求体 {mistakeIds: [1, 2, 3]}
     * @return 成功删除的数量
     */
    @DeleteMapping("/delete")
    public BaseResponse<Integer> deleteMistakes(@Valid @RequestBody MistakeIdsRequest request) {
        try {
            int count = mistakeBookService.batchDelete(request.getMistakeIds());
            return ResultUtil.success(count);
        } catch (Exception e) {
            log.error("删除错题失败", e);
            return ResultUtil.error("删除错题失败: " + e.getMessage());
        }
    }

    /**
     * 获取复习推荐（基于遗忘曲线）
     *
     * @return 推荐复习的错题列表
     */
    @GetMapping("/review-recommendation")
    public BaseResponse<List<com.review.agent.entity.vo.ReviewRecommendationVO>> getReviewRecommendation() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            List<ReviewRecommendationVO> recommendations = mistakeBookService.getReviewRecommendations(userId);
            return ResultUtil.success(recommendations);
        } catch (Exception e) {
            log.error("获取复习推荐失败", e);
            return ResultUtil.error("获取复习推荐失败: " + e.getMessage());
        }
    }
}
