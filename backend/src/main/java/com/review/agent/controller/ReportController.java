package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.request.ReportRequest;
import com.review.agent.entity.vo.ReportVo;
import com.review.agent.service.ReportService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

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
    public BaseResponse<Map<String,Integer>> wordReport(@RequestHeader("userId") Long userId) {
        Map<String, Integer> resultMap = reportService.generateWordReport(userId);
        return ResultUtil.success(resultMap);
    }
}