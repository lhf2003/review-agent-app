package com.review.agent.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.review.agent.common.CommonConstant;
import com.review.agent.entity.AnalysisResult;
import com.review.agent.entity.FileInfo;
import com.review.agent.entity.UserInfo;
import com.review.agent.entity.request.AnalysisRequest;
import com.review.agent.repository.AnalysisResultRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class AnalysisService {
    @Resource
    private AnalysisResultRepository analysisResultRepository;
    @Resource
    private FileInfoService fileInfoService;
    @Resource
    private UserService userService;
    @Resource
    private CompiledGraph compiledGraph;


    /**
     * 开始分析
     * @param analysisRequest 分析请求
     * @throws NoSuchElementException 如果用户不存在或文件不存在
     */
    public void startAnalysis(AnalysisRequest analysisRequest){
        Long userId = analysisRequest.getUserId();
        UserInfo userInfo = userService.findById(userId);
        if (userInfo == null) {
            throw new IllegalArgumentException("user not found");
        }
        List<Long> fileIdList = analysisRequest.getFileIdList();

        for (Long fileId : fileIdList) {
            FileInfo fileInfo = fileInfoService.findById(fileId);
            if (fileInfo == null) {
                log.error("file info not found, fileId: {}", fileId);
                continue;
            }
            Map<String, Object> metaMap = Map.of("fileId", fileId);
            // 调用图计算引擎
            Optional<OverAllState> callResult = compiledGraph.call(metaMap);
            callResult.ifPresent(overAllState -> processAnalysisResult(overAllState, fileInfo));
        }
    }

    /**
     * 处理分析结果
     * @param overAllState 图计算引擎返回的分析结果
     * @param fileInfo 文件信息
     */
    private void processAnalysisResult(OverAllState overAllState, FileInfo fileInfo) {
        Optional<Object> problemStatement = overAllState.value("problem_statement");
        Optional<Object> solution = overAllState.value("solution");

        AnalysisResult analysisResult = new AnalysisResult();
        analysisResult.setFileId(fileInfo.getId());
//                    analysisResult.setUserId(userId);

        if (problemStatement.isPresent() && solution.isPresent()) {
            String problemStatementStr = problemStatement.get().toString();
            String solutionStr = solution.get().toString();

            fileInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_PROCESSED);

            analysisResult.setProblemStatement(problemStatementStr);
            analysisResult.setSolution(solutionStr);
            analysisResult.setStatus(CommonConstant.ANALYSIS_STATUS_PROCESSED);
            analysisResult.setCreatedTime(new Date());
        } else {
            fileInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_ERROR);
            analysisResult.setStatus(CommonConstant.ANALYSIS_STATUS_ERROR);
        }
        // 更新文件处理状态
        fileInfoService.update(fileInfo);
        // 保存AI分析结果
        analysisResultRepository.save(analysisResult);
    }
}