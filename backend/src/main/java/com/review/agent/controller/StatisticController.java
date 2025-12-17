package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
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

    /**
     * 词云
     * @param userId 用户id
     * @return 名称和对应数量的映射
     */
    @PostMapping("/word-cloud")
    public BaseResponse<Map<String, Integer>> generateWordCloud(@RequestHeader("userId") Long userId,@RequestBody StatisticRequest request) {
        Map<String, Integer> resultMap = statisticService.generateWordCloud(userId, request);
        return ResultUtil.success(resultMap);
    }

    /**
     * 统计指定时间范围内的标签使用趋势
     * @param userId 用户ID
     * @param request 查询参数
     * @return <日期, 统计信息>
     */
    @PostMapping("/tag/trend")
    public BaseResponse<Map<String, List<StatisticVo>>> getDateTagCountTrend(@RequestHeader("userId") Long userId,
                                                                             @RequestBody StatisticRequest request) {
        request.setUserId(userId);
        return ResultUtil.success(statisticService.getDateTagCountTrend(request));
    }
}