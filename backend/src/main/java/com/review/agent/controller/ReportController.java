package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.service.ReportService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequestMapping("/report")
@RestController
public class ReportController {
    @Resource
    private ReportService reportService;

    /**
     * 词云
     * @param userId 用户id
     * @return 名称和对应数量的映射
     */
    @GetMapping("/word")
    public BaseResponse<Map<String,Integer>> generateWordCloud(@RequestHeader("userId") Long userId) {
        Map<String, Integer> resultMap = reportService.generateWordCloud(userId);
        return ResultUtil.success(resultMap);
    }
}