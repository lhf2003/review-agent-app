package com.review.agent.service;

import com.review.agent.entity.pojo.AnalysisResult;
import com.review.agent.entity.pojo.DataInfo;
import com.review.agent.entity.vo.SessionTraceVo;
import com.review.agent.repository.AnalysisResultRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SessionTraceService {
    @Resource
    private AnalysisResultRepository analysisResultRepository;
    @Resource
    private DataInfoService dataInfoService;

    public SessionTraceVo getInfo(Long userId, Long fileId) {
        SessionTraceVo sessionTraceVo = new SessionTraceVo();
        sessionTraceVo.setAnalysisResultInfoList(new ArrayList<>());
        // 文件内容
        DataInfo dataInfo = dataInfoService.findById(fileId);
        sessionTraceVo.setContent(dataInfo.getFileContent());

        // 文件分析结果
        List<AnalysisResult> analysisResultList = analysisResultRepository.findByUserIdAndDataId(userId, fileId);
        if (!CollectionUtils.isEmpty(analysisResultList)) {
            List<SessionTraceVo.AnalysisResultInfo> analysisResultInfoList = sessionTraceVo.getAnalysisResultInfoList();
            analysisResultList.forEach(analysisResult -> {
                SessionTraceVo.AnalysisResultInfo analysisResultInfo = new SessionTraceVo.AnalysisResultInfo();
                analysisResultInfo.setProblemStatement(analysisResult.getProblemStatement());
                analysisResultInfo.setSolution(analysisResult.getSolution());
                analysisResultInfo.setStartIndex(analysisResult.getSessionStart());
                analysisResultInfo.setEndIndex(analysisResult.getSessionEnd());
                analysisResultInfoList.add(analysisResultInfo);
            });
        }
        return sessionTraceVo;
    }
}