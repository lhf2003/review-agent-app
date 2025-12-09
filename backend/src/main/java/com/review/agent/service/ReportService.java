package com.review.agent.service;

import com.review.agent.entity.AnalysisResult;
import com.review.agent.entity.AnalysisTag;
import com.review.agent.entity.MainTag;
import com.review.agent.entity.SubTag;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.AnalysisTagRepository;
import com.review.agent.repository.MainTagRepository;
import com.review.agent.repository.SubTagRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
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

    public Map<String, Integer> generateWordReport(Long userId) {
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
            resultMap.merge(mainTagMap.get(analysisTag.getId()), 1, Integer::sum);

            String[] subTagIds = analysisTag.getSubTagId().split(",");
            for (String subTagId : subTagIds) {
                resultMap.merge(subTagMap.get(Long.valueOf(subTagId)), 1, Integer::sum);
            }
        });
        return resultMap;
    }

    public void generateDailyReport(Long userId) {

    }

    public void generateWeeklyReport(Long userId) {

    }
}