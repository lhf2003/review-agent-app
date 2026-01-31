package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.request.StatisticRequest;
import com.review.agent.entity.vo.StatisticVo;
import com.review.agent.service.StatisticService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 统计信息
 */
@RestController
@RequestMapping("/statistic")
public class StatisticController {
    @Resource
    private StatisticService statisticService;
    @Resource
    private SecurityUtils securityUtils;

    /**
     * 词云
     * @param request 统计请求
     * @return 名称和对应数量的映射
     */
    @PostMapping("/word-cloud")
    public BaseResponse<Map<String, Integer>> generateWordCloud(@RequestBody StatisticRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        Map<String, Integer> resultMap = statisticService.generateWordCloud(userId, request);
        return ResultUtil.success(resultMap);
    }

    /**
     * 统计指定时间范围内的标签使用趋势
     * @param request 查询参数
     * @return <日期, 统计信息>
     */
    @PostMapping("/tag/trend")
    public BaseResponse<Map<String, List<StatisticVo>>> getDateTagCountTrend(@RequestBody StatisticRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        request.setUserId(userId);
        return ResultUtil.success(statisticService.getDateTagCountTrend(request));
    }
}