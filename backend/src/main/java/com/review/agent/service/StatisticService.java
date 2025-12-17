package com.review.agent.service;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.entity.pojo.*;
import com.review.agent.entity.request.StatisticRequest;
import com.review.agent.entity.vo.StatisticVo;
import com.review.agent.repository.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.sound.sampled.Line;
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
    private MainTagRepository mainTagRepository;
    @Resource
    private SubTagRepository subTagRepository;

    /**
     * 生成词云
     * @param userId 用户ID
     * @return 词云数据
     */
    public Map<String, Integer> generateWordCloud(Long userId, StatisticRequest request) {
        List<AnalysisResult> analysisResultList = analysisResultRepository.findAllByDate(userId, request.getStartDate().atStartOfDay(), request.getEndDate().atTime(23, 59, 59));
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

    public Map<String, List<StatisticVo>> getDateTagCountTrend(StatisticRequest request) {
        // <日期, 统计信息>
        Map<String, List<StatisticVo>> resultMap = new LinkedHashMap<>();

        // 查询用户所有主标签和子标签
        List<MainTag> mainTagList = mainTagRepository.findAllByUserId(request.getUserId());
        List<SubTag> subTagList = subTagRepository.findAllByUserId(request.getUserId());
        Map<Long, String> mainTagMap = mainTagList.stream().collect(Collectors.toMap(MainTag::getId, MainTag::getName));
        Map<Long, String> subTagMap = subTagList.stream().collect(Collectors.toMap(SubTag::getId, SubTag::getName));

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
            List<AnalysisTag> analysisTagList = analysisTagRepository.findAllByAnalysisResultId(analysisIdList);

            // 统计主标签和子标签数量 <标签名,数量>
            Map<String, Integer> tagNameToCountMap = new HashMap<>();
            for (AnalysisTag analysisTag : analysisTagList) {
                Long mainTagId = analysisTag.getTagId();
                String subTagId = analysisTag.getSubTagId();
                // 主标签
                if (mainTagId != null) {
                    String mainTagName = mainTagMap.get(mainTagId);
                    tagNameToCountMap.merge(mainTagName, 1, Integer::sum);
                }
                // 子标签
                if (StringUtils.hasText(subTagId)) {
                    List<Long> subTagIdList = Arrays.stream(subTagId.split(",")).map(Long::valueOf).toList();
                    for (Long id : subTagIdList) {
                        String subTagName = subTagMap.get(id);
                        tagNameToCountMap.merge(subTagName, 1, Integer::sum);
                    }
                }
            }

            resultMap.put(endFlagDate.toString(), tagNameToCountMap.entrySet().stream().map(e -> new StatisticVo(e.getKey(), e.getValue())).toList());
            endFlagDate = endFlagDate.plusDays(1);
        }
        return resultMap;
    }
}
