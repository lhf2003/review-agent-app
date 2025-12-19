package com.review.agent.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.common.constant.CommonConstant;
import com.review.agent.common.utils.ExceptionUtils;
import com.review.agent.entity.dto.NodeExecuteDto;
import com.review.agent.entity.pojo.*;
import com.review.agent.entity.projection.AnalysisResultInfo;
import com.review.agent.entity.request.AnalysisResultRequest;
import com.review.agent.entity.vo.AnalysisResultVo;
import com.review.agent.entity.vo.AnalysisTagVo;
import com.review.agent.repository.AnalysisResultRepository;
import com.review.agent.repository.AnalysisTagRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
    private CompiledGraph analysisCompiledGraph;
    @Resource
    private SseService sseService;
    @Resource
    private VectorStoreService vectorStoreService;

    /**
     * 开始分析
     */
    @Transactional(rollbackFor = Exception.class)
    public void startAnalysis(Long userId, Long fileId) {
        UserInfo userInfo = userService.findById(userId);
        if (userInfo == null) {
            throw new IllegalArgumentException("user not found");
        }

        DataInfo dataInfo = fileInfoService.findById(fileId);
        if (dataInfo == null) {
            log.error("file info not found, fileId: {}", fileId);
            ExceptionUtils.throwDataNotFound("file info not found, fileId: " + fileId);
        }
        dataInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_PROCESSING);
        fileInfoService.update(dataInfo);

        new Thread(() -> {
            sseService.sendLog(userId, "🚀 开始分析文件: " + dataInfo.getFileName());

            Map<String, Object> metaMap = new HashMap<>();
            metaMap.put("fileId", fileId);
            metaMap.put("userId", userId);
            metaMap.put("originalContent", dataInfo.getFileContent());

            sseService.sendLog(userId, "🤖 正在执行AI分析流...");
            // 调用图计算引擎
            RunnableConfig config = RunnableConfig.builder()
                    .threadId("analysis-graph-" + userId)
                    .build();
            Optional<OverAllState> callResult = analysisCompiledGraph.invoke(metaMap, config);
            callResult.ifPresent(overAllState -> processAnalysisResult(overAllState, dataInfo));

            sseService.sendLog(userId, "✅ 分析完成: " + dataInfo.getFileName());
        }).start();

    }

    /**
     * 处理分析结果
     * @param overAllState 图计算引擎返回的分析结果
     * @param dataInfo 文件信息
     */
    public void processAnalysisResult(OverAllState overAllState, DataInfo dataInfo) {
        Optional<Object> nodeResultObj = overAllState.value("nodeResult");

        List<NodeExecuteDto> nodeExecuteDtoList = null;

        List<AnalysisResult> analysisResultList = new ArrayList<>();
        List<AnalysisTag> analysisTagList = new ArrayList<>();

        // 安全地转换对象类型
        if (nodeResultObj.isPresent() && nodeResultObj.get() instanceof List<?> rawList) {
            try {
                nodeExecuteDtoList = rawList.stream()
                        .map(item -> objectMapper.convertValue(item, NodeExecuteDto.class))
                        .toList();
            } catch (Exception e) {
                log.error("Failed to convert nodeExecuteDtoList to List<NodeExecuteDto>", e);
            }
        }

        // 封装结果
        if (!CollectionUtils.isEmpty(nodeExecuteDtoList)) {
            dataInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_PROCESSED);
            for (NodeExecuteDto executeDto : nodeExecuteDtoList) {
                String vectorId = addVectorToRedis(executeDto);
                buildAnalysisResult(vectorId, executeDto, analysisResultList, analysisTagList);
            }
        } else {
            dataInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_ERROR);
        }

        // 更新文件处理状态
        fileInfoService.update(dataInfo);

        analysisResultRepository.saveAll(analysisResultList);
        for (int i = 0; i < analysisTagList.size(); i++) {
            AnalysisTag analysisTag = analysisTagList.get(i);
            analysisTag.setAnalysisId(analysisResultList.get(i).getId());
        }
        analysisTagRepository.saveAll(analysisTagList);

        log.info("分析结束");
    }

    /**
     * 添加到Redis向量数据库
     * @param executeDto 节点执行结果
     * @return 向量ID
     */
    private String addVectorToRedis(NodeExecuteDto executeDto) {
        Map<String, Object> metaDataMap = new HashMap<>();
        metaDataMap.put("userId", executeDto.getUserId().toString());
        metaDataMap.put("fileId", executeDto.getFileId().toString());
        metaDataMap.put("solution", executeDto.getSolution());
        metaDataMap.put("sessionContent", executeDto.getSessionContent());

        String vectorId = UUID.randomUUID().toString();
        Document document = new Document(vectorId, executeDto.getProblemStatement(), metaDataMap);
        vectorStoreService.addOne(document);
        return vectorId;
    }

    private void buildAnalysisResult(String vectorId, NodeExecuteDto executeDto, List<AnalysisResult> analysisResultList, List<AnalysisTag> analysisTagList) {
        AnalysisResult analysisResult = new AnalysisResult();
        analysisResult.setVectorId(vectorId);
        analysisResult.setUserId(executeDto.getUserId());
        analysisResult.setFileId(executeDto.getFileId());
        analysisResult.setProblemStatement(executeDto.getProblemStatement());
        analysisResult.setSolution(executeDto.getSolution());
        analysisResult.setSessionStart(executeDto.getSessionStart());
        analysisResult.setSessionEnd(executeDto.getSessionEnd());
        analysisResult.setSessionContent(executeDto.getSessionContent());
        analysisResult.setStatus(executeDto.getStatus());
        analysisResult.setCreatedTime(LocalDateTime.now());
        analysisResultList.add(analysisResult);

        AnalysisTag analysisTag = new AnalysisTag();
        analysisTag.setTagId(executeDto.getTagId());
        analysisTag.setSubTagId(executeDto.getSubTagId());
        analysisTag.setRecommends(executeDto.getRecommends());
        analysisTagList.add(analysisTag);
    }


    public List<AnalysisTagVo> getTagList(Long userId) {
        List<AnalysisTagVo> resultList = new ArrayList<>();

        // 根据用户ID查询标签id到分析结果的映射
        List<AnalysisResult> analysisResultList = analysisResultRepository.findByUserId(userId);
        List<Long> analysisIdList = analysisResultList.stream().map(AnalysisResult::getId).toList();
        List<AnalysisTag> analysisTagList = analysisTagRepository.findByAnalysisIdIn(analysisIdList);
        // 过滤空标签
        analysisTagList = analysisTagList.stream().filter(item -> item.getTagId() != null).toList();

        List<Long> mainTagIdList = analysisTagList.stream()
                .filter(item -> item.getTagId() != null)
                .map(AnalysisTag::getTagId)
                .distinct()
                .toList();
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
        subTagIdStringList.forEach(item -> {
            if (StringUtils.hasText(item)) {
                List<Long> list = Arrays.stream(item.trim().split(","))
                        .map(Long::parseLong)
                        .toList();
                subTagIdList.addAll(list);
            }
        });
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
            vo.setFileName(item.getFileName());
            vo.setCreateTime(item.getCreateTime());
            // 转换子标签ID为标签名列表
            if (item.getSubTagIds() != null && !item.getSubTagIds().isEmpty()) {
                List<String> subTagNameList = Arrays.stream(item.getSubTagIds().split(","))
                        .map(Long::parseLong)
                        .map(subTagIdToNameMap::get)
                        .toList();
                vo.setSubTagNameList(subTagNameList);
            }
            if (item.getRecommendTag() != null && !item.getRecommendTag().isEmpty()) {
                vo.setRecommendTagList(Arrays.stream(item.getRecommendTag().split(",")).toList());
            }
            resultList.add(vo);
        });

        return resultList;
    }

    public AnalysisResult getAnalysisResult(Long userId, Long dataId, Long analysisId) {
        return analysisResultRepository.findByCondition(userId, dataId, analysisId);
    }

    public Map<String, List<AnalysisResultVo>> getFileNameList(Long userId) {
        List<AnalysisResultInfo> list = analysisResultRepository.findByPage(Pageable.unpaged(), null, null, null, userId);

        // 构建子标签ID到名称的映射
        List<SubTag> subTagList = tagService.findSubTagList(userId);
        Map<Long, String> subTagIdToNameMap = subTagList.stream().collect(Collectors.toMap(SubTag::getId, SubTag::getName));

        List<AnalysisResultVo> voList = list.stream().map(item -> {
            AnalysisResultVo vo = new AnalysisResultVo();
            vo.setId(item.getId());
            vo.setFileId(item.getFileId());
            vo.setProblemStatement(item.getProblemStatement());
            vo.setMainTagName(item.getTagName());
            vo.setFileName(item.getFileName().substring(0, item.getFileName().lastIndexOf(".")));
            vo.setCreateTime(item.getCreateTime());
            // 转换子标签ID为标签名列表
            if (item.getSubTagIds() != null && !item.getSubTagIds().isEmpty()) {
                List<String> subTagNameList = Arrays.stream(item.getSubTagIds().split(","))
                        .map(Long::parseLong)
                        .map(subTagIdToNameMap::get)
                        .toList();
                vo.setSubTagNameList(subTagNameList);
            }
            if (item.getRecommendTag() != null && !item.getRecommendTag().isEmpty()) {
                vo.setRecommendTagList(Arrays.stream(item.getRecommendTag().split(",")).toList());
            }
            return vo;
        }).toList();
        return voList.stream().collect(Collectors.groupingBy(
                AnalysisResultVo::getFileName,
                () -> new TreeMap<>(Comparator.reverseOrder()),
                Collectors.toList()));
    }
}