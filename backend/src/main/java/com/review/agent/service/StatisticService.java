package com.review.agent.service;

import com.review.agent.entity.pojo.*;
import com.review.agent.entity.request.StatisticRequest;
import com.review.agent.entity.vo.StatisticVo;
import com.review.agent.repository.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计数据service
 */
@Slf4j
@Service
public class StatisticService {
    @Resource
    private AnalysisResultRepository analysisResultRepository;
    @Resource
    private AnalysisTagRepository analysisTagRepository;
    @Resource
    private TagRepository tagRepository;

    /**
     * 生成词云
     * @param userId 用户ID
     * @return 词云数据
     */
    public Map<String, Integer> generateWordCloud(Long userId, StatisticRequest request) {
        List<AnalysisResult> analysisResultList = analysisResultRepository.findAllByDate(userId, request.getStartDate().atStartOfDay(), request.getEndDate().atTime(23, 59, 59));
        List<Long> analysisResultIdList = analysisResultList.stream().map(AnalysisResult::getId).toList();
        List<AnalysisTag> analysisTags = analysisTagRepository.findByAnalysisResultIdIn(analysisResultIdList);
        List<Tag> tagList = tagRepository.findByUserId(userId);

        // <id,name>
        Map<Long, String> tagMap = tagList.stream().collect(Collectors.toMap(Tag::getId, Tag::getName));

        Map<String, Integer> resultMap = new HashMap<>();
        analysisTags.forEach(analysisTag -> {
            if (analysisTag.getTagId() != null) {
                String tagName = tagMap.get(analysisTag.getTagId());
                if (StringUtils.hasText(tagName)) {
                    resultMap.merge(tagName, 1, Integer::sum);
                }
            }
        });
        return resultMap;
    }

    public Map<String, List<StatisticVo>> getDateTagCountTrend(StatisticRequest request) {
        // <日期, 统计信息>
        Map<String, List<StatisticVo>> resultMap = new LinkedHashMap<>();

        // 查询用户所有标签
        List<Tag> tagList = tagRepository.findByUserId(request.getUserId());
        Map<Long, String> tagMap = tagList.stream().collect(Collectors.toMap(Tag::getId, Tag::getName));

        // 指定时间范围内的数据
        List<AnalysisResult> allByDate = analysisResultRepository.findAllByDate(request.getUserId(), request.getStartDate().atStartOfDay(), request.getEndDate().atStartOfDay());
        Map<LocalDate, List<AnalysisResult>> dateToResultMap = allByDate.stream().collect(Collectors.groupingBy(a -> a.getCreatedTime().toLocalDate()));

        LocalDate endFlagDate = request.getStartDate();
        while (endFlagDate.isBefore(request.getEndDate())) {
            List<AnalysisResult> analysisResultList = dateToResultMap.get(endFlagDate);
            if (CollectionUtils.isEmpty(analysisResultList)) {
                resultMap.put(endFlagDate.toString(), List.of());
                endFlagDate = endFlagDate.plusDays(1);
                continue;
            }
            List<Long> analysisIdList = analysisResultList.stream().map(AnalysisResult::getId).toList();
            List<AnalysisTag> analysisTagList = analysisTagRepository.findByAnalysisResultIdIn(analysisIdList);

            // 统计标签数量 <标签名,数量>
            Map<String, Integer> tagNameToCountMap = new HashMap<>();
            for (AnalysisTag analysisTag : analysisTagList) {
                Long tagId = analysisTag.getTagId();
                if (tagId != null) {
                    String tagName = tagMap.get(tagId);
                    if (StringUtils.hasText(tagName)) {
                        tagNameToCountMap.merge(tagName, 1, Integer::sum);
                    }
                }
            }

            resultMap.put(endFlagDate.toString(), tagNameToCountMap.entrySet().stream().map(e -> new StatisticVo(e.getKey(), e.getValue())).toList());
            endFlagDate = endFlagDate.plusDays(1);
        }
        return resultMap;
    }
}
