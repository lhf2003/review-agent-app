package com.review.agent.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.review.agent.entity.projection.AnalysisResultInfo;
import com.review.agent.common.constant.CommonConstant;
import com.review.agent.entity.*;
import com.review.agent.entity.request.AnalysisRequest;
import com.review.agent.entity.request.AnalysisResultRequest;
import com.review.agent.entity.vo.AnalysisResultVo;
import com.review.agent.entity.vo.AnalysisTagVo;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.AnalysisTagRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AnalysisService {
    @Resource
    private AnalysisResultRepository analysisResultRepository;
    @Resource
    private AnalysisTagRepository analysisTagRepository;
    @Resource
    private DataInfoService fileInfoService;
    @Resource
    private TagService tagService;
    @Resource
    private UserService userService;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private CompiledGraph compiledGraph;


    /**
     * 开始分析
     * @param analysisRequest 分析请求
     * @throws NoSuchElementException 如果用户不存在或文件不存在
     */
    public void startAnalysis(AnalysisRequest analysisRequest) {
        Long userId = analysisRequest.getUserId();
        UserInfo userInfo = userService.findById(userId);
        if (userInfo == null) {
            throw new IllegalArgumentException("user not found");
        }
        List<Long> fileIdList = analysisRequest.getFileIdList();

        for (Long fileId : fileIdList) {
            DataInfo dataInfo = fileInfoService.findById(fileId);
            if (dataInfo == null) {
                log.error("file info not found, fileId: {}", fileId);
                continue;
            }
            Map<String, Object> metaMap = new HashMap<>();
            metaMap.put("fileId", fileId);
            metaMap.put("userId", userId);
            metaMap.put("content", dataInfo.getFileContent());
            // 调用图计算引擎
            Optional<OverAllState> callResult = compiledGraph.call(metaMap);
            callResult.ifPresent(overAllState -> processAnalysisResult(overAllState, dataInfo));
        }
    }

    /**
     * 处理分析结果
     * @param overAllState 图计算引擎返回的分析结果
     * @param dataInfo 文件信息
     */
    @Transactional
    public void processAnalysisResult(OverAllState overAllState, DataInfo dataInfo) {
        Optional<Object> analysisResultObj = overAllState.value("analysisResultList");
        Optional<Object> analysisTagObj = overAllState.value("analysisTagList");

        List<AnalysisResult> analysisResultList = null;
        List<AnalysisTag> analysisTagList = null;

        // 安全地转换对象类型
        if (analysisResultObj.isPresent() && analysisResultObj.get() instanceof List<?> rawList) {
            try {
                rawList = (List<?>) analysisResultObj.get();
                analysisResultList = rawList.stream()
                        .map(item -> objectMapper.convertValue(item, AnalysisResult.class))
                        .toList();
            } catch (Exception e) {
                log.error("Failed to convert analysisResultList to List<AnalysisResult>", e);
            }
        }

        if (analysisTagObj.isPresent() && analysisTagObj.get() instanceof List<?> rawList) {
            rawList = (List<?>) analysisTagObj.get();
            analysisTagList = rawList.stream()
                    .map(item -> objectMapper.convertValue(item, AnalysisTag.class))
                    .toList();
        }

        if (analysisResultList != null && analysisTagList != null) {
            dataInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_PROCESSED);
        } else {
            dataInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_ERROR);
        }

        // 更新文件处理状态
        fileInfoService.update(dataInfo);
        if (!CollectionUtils.isEmpty(analysisResultList)) {
            analysisResultRepository.saveAll(analysisResultList);
            for (int i = 0; i < analysisTagList.size(); i++) {
                AnalysisTag analysisTag = analysisTagList.get(i);
                analysisTag.setAnalysisId(analysisResultList.get(i).getId());
            }
            analysisTagRepository.saveAll(analysisTagList);
        }
        log.info("分析结束");
    }


    public List<AnalysisTagVo> getTagList(Long userId) {
        List<AnalysisTagVo> resultList = new ArrayList<>();

        // 根据用户ID查询标签id到分析结果的映射
        List<AnalysisResult> analysisResultList = analysisResultRepository.findByUserId(userId);
        List<Long> analysisIdList = analysisResultList.stream().map(AnalysisResult::getId).toList();
        List<AnalysisTag> analysisTagList = analysisTagRepository.findByAnalysisIdIn(analysisIdList);
        List<Long> mainTagIdList = analysisTagList.stream().map(AnalysisTag::getTagId).toList();
        List<MainTag> mainTagList = tagService.findByIdList(mainTagIdList);
        // 统计每个标签的出现次数
        Map<Long, Long> countMap = analysisTagList.stream().collect(Collectors.groupingBy(AnalysisTag::getTagId, Collectors.counting()));
        // 构建标签ID到名称的映射
        Map<Long, String> tagIdToNameMap = mainTagList.stream().collect(Collectors.toMap(MainTag::getId, MainTag::getName));

        // 构建分析标签VO
        for (Map.Entry<Long, String> entry : tagIdToNameMap.entrySet()) {
            Long key = entry.getKey();
            String value = entry.getValue();
            AnalysisTagVo tagVo = new AnalysisTagVo();
            tagVo.setTagId(key);
            tagVo.setTagName(value);
            tagVo.setCount(countMap.get(key).intValue());
            tagVo.setType("main");
            resultList.add(tagVo);
        }

        // 构建子标签id列表
        List<String> subTagIdStringList = analysisTagList.stream().map(AnalysisTag::getSubTagId).toList();
        List<Long> subTagIdList = new ArrayList<>();
        subTagIdStringList.forEach(item -> subTagIdList.addAll(Arrays.stream(item.split(",")).map(Long::parseLong).toList()));
        // 去重
        List<Long> distinctSubTagIdList = subTagIdList.stream().distinct().toList();

        // 构建<id,name>映射
        List<SubTag> subTags = tagService.findSubTagList(userId);
        Map<Long, String> subTagMap = subTags.stream().collect(Collectors.toMap(SubTag::getId, SubTag::getName));

        // 构建子标签VO列表
        distinctSubTagIdList.forEach(id -> {
            AnalysisTagVo tagVo = new AnalysisTagVo();
            long count = subTagIdList.stream().filter(item -> item.equals(id)).count();
            tagVo.setTagId(id);
            tagVo.setTagName(subTagMap.get(id));
            tagVo.setCount((int) count);
            tagVo.setType("sub");
            resultList.add(tagVo);
        });

        return resultList;
    }


    public List<AnalysisResultVo> page(Pageable pageable, AnalysisResultRequest resultRequest) {
        // 分页查询分析结果
        List<AnalysisResultInfo> page = analysisResultRepository.findByPage(pageable, resultRequest.getFileId(), resultRequest.getProblemStatement(), resultRequest.getTagId()
                , resultRequest.getUserId());

        // 构建子标签ID到名称的映射
        List<SubTag> subTagList = tagService.findSubTagList(resultRequest.getUserId());
        Map<Long, String> subTagIdToNameMap = subTagList.stream().collect(Collectors.toMap(SubTag::getId, SubTag::getName));

        // 构建分析结果VO列表
        List<AnalysisResultVo> resultList = new ArrayList<>(page.size());
        page.forEach(item -> {
            AnalysisResultVo vo = new AnalysisResultVo();
            vo.setId(item.getId());
            vo.setFileId(item.getFileId());
            vo.setProblemStatement(item.getProblemStatement());
            vo.setMainTagName(item.getTagName());
            // 转换子标签ID为标签名列表
            if (item.getSubTagIds() != null && !item.getSubTagIds().isEmpty()) {
                List<String> subTagNameList = Arrays.stream(item.getSubTagIds().split(","))
                        .map(Long::parseLong)
                        .map(subTagIdToNameMap::get)
                        .toList();
                vo.setSubTagNameList(subTagNameList);
            }
            resultList.add(vo);
        });

        return resultList;
    }

    public AnalysisResult getAnalysisResult(Long userId, Long dataId, Long analysisId) {
        return analysisResultRepository.findByCondition(userId, dataId, analysisId);
    }

}