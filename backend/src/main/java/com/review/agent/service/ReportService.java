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

    public void generateDailyReport(Long userId) {
        UserInfo userInfo = userService.findById(userId);
        if (!StringUtils.hasText(userInfo.getEmail())) {
            log.error("用户 {} 没有绑定邮箱", userId);
            return;
        }
        // 构建日报范围
        LocalDate today = LocalDate.now();
        List<AnalysisResult> analysisResultList = analysisResultRepository.findAllByDate(userId, today.atStartOfDay(), today.atStartOfDay());

        if (CollectionUtils.isEmpty(analysisResultList)) {
            log.info("用户 {} 当天没有分析结果，无法生成日报", userId);
            return;
        }
        String report = buildBasicReport(analysisResultList);

        // 存入数据库
        ReportData reportData = new ReportData();
        reportData.setUserId(userId);
        reportData.setReportContent(report);
        reportData.setType(1);
        reportData.setStartDate(Date.from(today.atStartOfDay().toInstant(ZoneOffset.UTC)));
        reportData.setEndDate(Date.from(today.atStartOfDay().toInstant(ZoneOffset.UTC)));
        reportData.setCreateTime(new Date());
        reportDataRepository.save(reportData);

        // 发送邮件
        MailUtils.sendReport(userInfo.getEmail(), report);
    }

    public void generateWeeklyReport(Long userId) {
        UserInfo userInfo = userService.findById(userId);
        if (!StringUtils.hasText(userInfo.getEmail())) {
            log.error("用户 {} 没有绑定邮箱", userId);
            return;
        }
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.minusDays(7);
        List<AnalysisResult> analysisResultList = analysisResultRepository.findAllByDate(userId, startDate.atStartOfDay(), endDate.atStartOfDay());
        String report = buildBasicReport(analysisResultList);

        // 存入数据库
        ReportData reportData = new ReportData();
        reportData.setUserId(userId);
        reportData.setReportContent(report);
        reportData.setType(2);
        reportData.setStartDate(Date.from(startDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
        reportData.setEndDate(Date.from(endDate.atStartOfDay().toInstant(ZoneOffset.UTC)));
        reportData.setCreateTime(new Date());
        reportDataRepository.save(reportData);

        // 发送邮件
        MailUtils.sendReport(userInfo.getEmail(), report);

    }

    private String buildBasicReport(List<AnalysisResult> analysisResultList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (AnalysisResult analysisResult : analysisResultList) {
            stringBuilder.append("# ").append(analysisResult.getProblemStatement()).append("\n");
            stringBuilder.append(analysisResult.getSolution()).append("\n");
        }
        String dailyReportPrompt = promptService.getDailyReportPrompt("");
        String response = analysisChatClient.prompt()
                .system(dailyReportPrompt)
                .user(stringBuilder.toString())
                .call()
                .content();
        if (StringUtils.hasText(response)) {
            response = formatResult(response);
            JSONObject jsonObject = JSON.parseObject(response);
            return jsonObject.getString("daily_report_html");
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

    public List<ReportData> getReport(Long userId, Integer type, String date) {
        List<ReportData> reportDataList = reportDataRepository.findByCondition(userId, type, date);
        if (CollectionUtils.isEmpty(reportDataList)) {
            return Collections.emptyList();
        }
        return reportDataList;
    }
}