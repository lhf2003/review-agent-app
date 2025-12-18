package com.review.agent.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.review.agent.common.utils.MailUtils;
import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.pojo.ReportData;
import com.review.agent.entity.pojo.UserInfo;
import com.review.agent.repository.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.review.agent.common.constant.CommonConstant.DAILY_REPORT;
import static com.review.agent.common.constant.CommonConstant.WEEKLY_REPORT;

@Slf4j
@Service
public class ReportService {
    @Resource
    private AnalysisTagRepository analysisTagRepository;

    @Resource
    private MainTagRepository mainTagRepository;

    @Resource
    private SubTagRepository subTagRepository;

    @Resource
    private AnalysisResultRepository analysisResultRepository;

    @Resource
    private PromptService promptService;

    @Resource
    private ReportDataRepository reportDataRepository;

    @Resource
    private UserService userService;

    @Resource
    private CompiledGraph reportCompiledGraph;

    @Resource
    private ChatClient analysisChatClient;

    /**
     * 获取报告列表
     * @param userId 用户ID
     * @param type 报告类型
     * @param date 日期
     * @return 报告列表
     */
    public List<ReportData> getReport(Long userId, Integer type, String date) {
        List<ReportData> reportDataList = reportDataRepository.findByCondition(userId, type, date);
        if (CollectionUtils.isEmpty(reportDataList)) {
            return Collections.emptyList();
        }
        return reportDataList;
    }

    /**
     * 生成报告
     * @param userId 用户ID
     * @param type 报告类型
     */
    public void generateReport(Long userId, int type) {
        // 检查用户是否绑定邮箱
        UserInfo userInfo = userService.findById(userId);
        if (!StringUtils.hasText(userInfo.getEmail())) {
            log.error("用户 {} 没有绑定邮箱", userId);
            return;
        }

        ReportData reportData = new ReportData();
        reportData.setUserId(userId);
        reportData.setType(type);
        reportData.setCreateTime(new Date());
        String report = switch (type) {
            case DAILY_REPORT -> generateDailyReport(userId, reportData);
            case WEEKLY_REPORT -> generateWeeklyReport(userId, reportData);
            default -> {
                log.error("未知的报告类型: {}", type);
                yield null;
            }
        };
        if (report == null) {
            return;
        }
        reportData.setReportContent(report);
        reportDataRepository.save(reportData);

        // 发送邮件
        MailUtils.sendReport(userInfo.getEmail(), report);
    }

    /**
     * 日报
     * @param userId 用户ID
     * @param reportData 报告数据
     * @return 日报内容
     */
    public String generateDailyReport(Long userId, ReportData reportData) {

        // 构建日报范围
        LocalDate today = LocalDate.now();
        List<AnalysisResult> analysisResultList = analysisResultRepository.findAllByDate(userId, today.atStartOfDay(), today.atStartOfDay());

        if (CollectionUtils.isEmpty(analysisResultList)) {
            log.info("用户 {} 当天没有分析结果，无法生成日报", userId);
            return "";
        }

        String dailyReportPrompt = promptService.getDailyReportPrompt("");
        String report = processReportContent(analysisResultList, dailyReportPrompt);

        reportData.setReportContent(report);
        reportData.setStartDate(Date.from(today.atStartOfDay().toInstant(ZoneOffset.UTC)));
        reportData.setEndDate(Date.from(today.atStartOfDay().toInstant(ZoneOffset.UTC)));
        return report;
    }

    /**
     * 周报
     * @param userId 用户ID
     * @param reportData 报告数据
     * @return 周报内容
     */
    public String generateWeeklyReport(Long userId, ReportData reportData) {

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);

        List<AnalysisResult> analysisResultList = analysisResultRepository.findAllByDate(userId, startDate.atStartOfDay(), endDate.atStartOfDay());
        if (CollectionUtils.isEmpty(analysisResultList)) {
            log.info("用户 {} 本周没有分析结果，无法生成周报", userId);
            return "";
        }

        String weeklyReportPrompt = promptService.getWeeklyReportPrompt("");
        String report = processReportContent(analysisResultList, weeklyReportPrompt);

        reportData.setReportContent(report);
        reportData.setStartDate(Date.from(startDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
        reportData.setEndDate(Date.from(endDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
        return report;
    }

    private String processReportContent(List<AnalysisResult> analysisResultList, String systemPrompt) {
        // 拼接会话
        StringBuilder stringBuilder = new StringBuilder();
        for (AnalysisResult analysisResult : analysisResultList) {
            stringBuilder.append("# ").append(analysisResult.getProblemStatement()).append("\n");
            stringBuilder.append(analysisResult.getSolution()).append("\n");
        }

        String response = analysisChatClient.prompt()
                .system(systemPrompt)
                .user(stringBuilder.toString())
                .call()
                .content();

        if (StringUtils.hasText(response)) {
            response = formatResult(response);
            JSONObject jsonObject = JSON.parseObject(response);
            return jsonObject.getString("report_html");
        }
        return null;
    }

    public String formatResult(String result) {
        if (result == null) {
            return "";
        }
        return result.replaceAll("^\\s*```json\\s*", "")   // 去除开头 ```json
                .replaceAll("^\\s*```\\s*", "")       // 或者可能是 ``` 开头
                .replaceAll("\\s*```\\s*$", "")
                .replace("\\n", "")// 去除结尾 ```
                .trim();
    }
}