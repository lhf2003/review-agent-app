package com.review.agent.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.review.agent.common.utils.MailUtils;
import com.review.agent.entity.AnalysisResult;
import com.review.agent.entity.AnalysisTag;
import com.review.agent.entity.MainTag;
import com.review.agent.entity.SubTag;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.AnalysisTagRepository;
import com.review.agent.repository.MainTagRepository;
import com.review.agent.repository.SubTagRepository;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            resultMap.merge(mainTagMap.get(analysisTag.getTagId()), 1, Integer::sum);

            String[] subTagIds = analysisTag.getSubTagId().split(",");
            for (String subTagId : subTagIds) {
                resultMap.merge(subTagMap.get(Long.valueOf(subTagId)), 1, Integer::sum);
            }
        });
        return resultMap;
    }

    public void generateDailyReport(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.withHour(0).withMinute(0).withSecond(0).withNano(0).minusDays(1);
        LocalDateTime endDate = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999).minusDays(1);
        List<AnalysisResult> analysisResultList = analysisResultRepository.findAllByDate(userId, startDate, endDate);
        String basicReport = buildBasicReport(userId, analysisResultList);
        JSONObject jsonObject = JSON.parseObject(basicReport);
        String dailyReportHtml = jsonObject.getString("daily_report_html");
        dailyReportHtml = dailyReportHtml.replace("\\n", "");
        System.out.println(dailyReportHtml);
        MailUtils.sendReport("2385107101@qq.com", dailyReportHtml);
    }

    public void generateWeeklyReport(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.withHour(0).withMinute(0).withSecond(0).withNano(0).minusDays(7);
        LocalDateTime endDate = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        List<AnalysisResult> analysisResultList = analysisResultRepository.findAllByDate(userId, startDate, endDate);
        String basicReport = buildBasicReport(userId, analysisResultList);
        basicReport = formatResult(basicReport);
        JSONObject jsonObject = JSON.parseObject(basicReport);
        String dailyReportHtml = jsonObject.getString("daily_report_html");
        dailyReportHtml = dailyReportHtml.replace("\\n", "");
        System.out.println(dailyReportHtml);
        MailUtils.sendReport("lhf97777@gmail.com", dailyReportHtml);

    }

    private String buildBasicReport(Long userId, List<AnalysisResult> analysisResultList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (AnalysisResult analysisResult : analysisResultList) {
            stringBuilder.append("# ").append(analysisResult.getProblemStatement()).append("\n");
            stringBuilder.append(analysisResult.getSolution()).append("\n");
        }

        String dailyReportPrompt = promptService.getDailyReportPrompt("");
        String content = analysisChatClient.prompt()
                .system(dailyReportPrompt)
                .user(stringBuilder.toString())
                .call()
                .content();
        return content;
    }

    public String formatResult(String result) {
        if (result == null) {
            return "";
        }
        return result.replaceAll("^\\s*```json\\s*", "")   // 去除开头 ```json
                .replaceAll("^\\s*```\\s*", "")       // 或者可能是 ``` 开头
                .replaceAll("\\s*```\\s*$", "")       // 去除结尾 ```
                .trim();
    }
}