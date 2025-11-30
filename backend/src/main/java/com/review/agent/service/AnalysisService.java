package com.review.agent.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.review.agent.common.constant.CommonConstant;
import com.review.agent.entity.*;
import com.review.agent.entity.request.AnalysisRequest;
import com.review.agent.entity.vo.AnalysisTagVo;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.AnalysisTagRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private void processAnalysisResult(OverAllState overAllState, DataInfo dataInfo) {
        Optional<String> problemStatement = overAllState.value("problem");
        Optional<String> solution = overAllState.value("solution");
        Optional<Long> tagId = overAllState.value("tagId");
        Optional<String> keywords = overAllState.value("keywords");

        AnalysisResult analysisResult = new AnalysisResult();
        analysisResult.setFileId(dataInfo.getId());
        analysisResult.setUserId(dataInfo.getUserId());

        if (problemStatement.isPresent() && solution.isPresent()) {
            String problemStatementStr = problemStatement.get();
            String solutionStr = solution.get();

            dataInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_PROCESSED);

            analysisResult.setProblemStatement(problemStatementStr);
            analysisResult.setSolution(solutionStr);
            analysisResult.setStatus(CommonConstant.ANALYSIS_STATUS_PROCESSED);
            analysisResult.setCreatedTime(new Date());
        } else {
            dataInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_ERROR);
            analysisResult.setStatus(CommonConstant.ANALYSIS_STATUS_ERROR);
        }


        // 更新文件处理状态
        fileInfoService.update(dataInfo);
        // 保存AI分析结果
        analysisResultRepository.save(analysisResult);
        // 保存分析标签
        if (tagId.isPresent()&&keywords.isPresent()) {
            AnalysisTag analysisTag = new AnalysisTag();
            Long tagIdValue = tagId.get();
            analysisTag.setTagId(tagIdValue);
            analysisTag.setAnalysisId(analysisResult.getId());
            analysisTag.setKeywords(keywords.get());
            analysisTagRepository.save(analysisTag);
        }
    }

    public List<AnalysisTagVo> getTagList(Long userId) {
        List<AnalysisTagVo> resultList = new ArrayList<>();

        // 根据用户ID查询标签id到分析结果的映射
        List<AnalysisResult> analysisResultList = analysisResultRepository.findByUserId(userId);
        Map<Long, List<AnalysisResult>> tagToResultMap = analysisResultList.stream().collect(Collectors.groupingBy(AnalysisResult::getTagId));

        // 用户的标签id到标签名的映射
        List<Tag> tagList = tagService.findAllByUserId(userId);
        Map<Long, String> tagIdToNameMap = tagList.stream().collect(Collectors.toMap(Tag::getId, Tag::getName));

        // 构建分析标签VO
        for (Map.Entry<Long, List<AnalysisResult>> entry : tagToResultMap.entrySet()) {
            AnalysisTagVo tagVo = new AnalysisTagVo();
            tagVo.setTagId(entry.getKey());
            String tagName = tagIdToNameMap.get(entry.getValue().get(0).getTagId());
            tagVo.setTagName(tagName);
            tagVo.setCount(entry.getValue().size());
            resultList.add(tagVo);
        }
        return resultList;
    }

    public List<AnalysisResult> page(Pageable pageable, AnalysisResult analysisResult) {
        return analysisResultRepository.findByPage(pageable, analysisResult.getProblemStatement(), analysisResult.getTagId()
                , analysisResult.getUserId(), analysisResult.getStatus());
    }

    public AnalysisResult getAnalysisResult(Long userId, Long dataId) {
        return analysisResultRepository.findByUserIdAndDataId(userId, dataId);
    }
}