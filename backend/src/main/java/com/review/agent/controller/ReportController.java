package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.pojo.ReportData;
import com.review.agent.service.ReportService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/report")
@RestController
public class ReportController {
    @Resource
    private ReportService reportService;

    /**
     * 获取报告
     * @param userId 用户id
     * @param type 报告类型（1：日报，2：周报）
     * @param date 日期（格式：yyyy-MM-dd）
     * @return 报告内容（html）
     */
    @GetMapping("/get")
    public BaseResponse<List<ReportData>> getReport(@RequestHeader("userId") Long userId,
                                                    @RequestParam(name = "type", defaultValue = "1") Integer type,
                                                    @RequestParam(name = "date", required = false) String date) {
        List<ReportData> reportList = reportService.getReport(userId, type, date);
        return ResultUtil.success(reportList);
    }
}