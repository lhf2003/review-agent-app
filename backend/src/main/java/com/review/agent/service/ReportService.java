package com.review.agent.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.review.agent.common.utils.MailUtils;
import com.review.agent.entity.pojo.*;
import com.review.agent.repository.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

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
     * 生成词云
     * @param userId 用户ID
     * @return 词云数据
     */
    public Map<String, Integer> generateWordCloud(Long userId) {
        List<AnalysisResult> analysisResultList = analysisResultRepository.findByUserId(userId);
        List<Long> analysisResultIdList = analysisResultList.stream().map(AnalysisResult::getId).toList();
        List<AnalysisTag> analysisTags = analysisTagRepository.findAllByAnalysisResultId(analysisResultIdList);
        List<MainTag> mainTagList = mainTagRepository.findAllByUserId(userId);
        List<SubTag> subTagList = subTagRepository.findAllByUserId(userId);

        // <id,name>
        Map<Long, String> mainTagMap = mainTagList.stream().collect(Collectors.toMap(MainTag::getId, MainTag::getName));
        Map<Long, String> subTagMap = subTagList.stream().collect(Collectors.toMap(SubTag::getId, SubTag::getName));

        Map<String, Integer> resultMap = new HashMap<>();
        analysisTags.forEach(analysisTag -> {
            if (analysisTag.getTagId() != null) {
                resultMap.merge(mainTagMap.get(analysisTag.getTagId()), 1, Integer::sum);
            }
            if (StringUtils.hasText(analysisTag.getSubTagId())) {
                String[] subTagIds = analysisTag.getSubTagId().split(",");
                for (String subTagId : subTagIds) {
                    resultMap.merge(subTagMap.get(Long.valueOf(subTagId)), 1, Integer::sum);
                }
            }
        });
        return resultMap;
    }

    public void generateDailyReport(Long userId) {
        UserInfo userInfo = userService.findById(userId);
        if (!StringUtils.hasText(userInfo.getEmail())) {
            log.error("用户 {} 没有绑定邮箱", userId);
            return;
        }
        // 构建日报范围
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.withHour(0).withMinute(0).withSecond(0).withNano(0).minusDays(2);
        LocalDateTime endDate = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999).minusDays(2);
        List<AnalysisResult> analysisResultList = analysisResultRepository.findAllByDate(userId, startDate, endDate);

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
        reportData.setStartDate(Date.from(startDate.toInstant(ZoneOffset.UTC)));
        reportData.setEndDate(Date.from(endDate.toInstant(ZoneOffset.UTC)));
        reportData.setCreateTime(new Date());
        reportDataRepository.save(reportData);

        // 发送邮件
        MailUtils.sendReport(userInfo.getEmail(), report);
    }

    public void generateWeeklyReport(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.withHour(0).withMinute(0).withSecond(0).withNano(0).minusDays(7);
        LocalDateTime endDate = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        List<AnalysisResult> analysisResultList = analysisResultRepository.findAllByDate(userId, startDate, endDate);
        String report = buildBasicReport(analysisResultList);

        // 存入数据库
        ReportData reportData = new ReportData();
        reportData.setUserId(userId);
        reportData.setReportContent(report);
        reportData.setType(2);
        reportData.setStartDate(Date.from(startDate.toInstant(ZoneOffset.UTC)));
        reportData.setEndDate(Date.from(endDate.toInstant(ZoneOffset.UTC)));
        reportData.setCreateTime(new Date());
        reportDataRepository.save(reportData);

        // 发送邮件
        MailUtils.sendReport("lhf97777@gmail.com", report);

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