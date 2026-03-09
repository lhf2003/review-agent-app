package com.review.agent.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.common.constant.CommonConstant;
import com.review.agent.common.utils.ExceptionUtils;
import com.review.agent.common.utils.JsonlUtils;
import com.review.agent.entity.dto.MultiDimensionTagResult;
import com.review.agent.entity.dto.NodeExecuteDto;
import com.review.agent.entity.pojo.*;
import com.review.agent.entity.projection.AnalysisResultInfo;
import com.review.agent.entity.request.AnalysisResultRequest;
import com.review.agent.entity.vo.AnalysisResultVo;
import com.review.agent.entity.vo.AnalysisTagVo;
import com.review.agent.entity.vo.SimilarAnalysisResultVo;
import com.review.agent.repository.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.concurrent.Executor;

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
    private AnalysisRecommendTagRepository analysisRecommendTagRepository;
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

    @Resource
    private TagRelationDiscoveryService tagRelationDiscoveryService;
    @Resource(name = "analysisTaskExecutor")
    private Executor analysisTaskExecutor;
    @Resource
    private TransactionTemplate transactionTemplate;

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

        // 使用线程池执行异步分析任务
        analysisTaskExecutor.execute(() -> {
            try {
                // 推送阶段1：开始解析文件
                sseService.sendStage(userId, 1);

                Map<String, Object> metaMap = new HashMap<>();
                metaMap.put("fileId", fileId);
                metaMap.put("userId", userId);
                // 如果是 JSONL 格式，提取 request 和 reply 字段拼接成字符串
                String fileContent = dataInfo.getFileContent();
                if (JsonlUtils.isJsonlFormat(fileContent)) {
                    StringBuilder conversationContent = new StringBuilder();
                    for (String line : fileContent.split("\\r?\\n")) {
                        if (line.trim().isEmpty()) continue;
                        try {
                            JSONObject record = JSON.parseObject(line);
                            String request = record.getString("request");
                            String reply = record.getString("reply");
                            if (request != null && !request.isEmpty()) {
                                conversationContent.append("用户请求：").append(request).append("\n");
                            }
                            if (reply != null && !reply.isEmpty()) {
                                conversationContent.append("模型回复：").append(reply).append("\n");
                            }
                            conversationContent.append("\n");
                        } catch (Exception e) {
                            log.warn("Failed to parse JSONL line: {}", line);
                        }
                    }
                    metaMap.put("originalContent", conversationContent.toString().trim());
                } else {
                    metaMap.put("originalContent", fileContent);
                }

                // 调用图计算引擎（内部会推送阶段2）
                RunnableConfig config = RunnableConfig.builder()
                        .threadId("analysis-graph-" + userId)
                        .build();
                Optional<OverAllState> callResult = analysisCompiledGraph.invoke(metaMap, config);
                callResult.ifPresent(overAllState ->
                    transactionTemplate.execute(status -> {
                        processAnalysisResult(overAllState, dataInfo);
                        return null;
                    })
                );

                // 推送阶段3：分析完成
                sseService.sendStage(userId, 3);
            } catch (Exception e) {
                log.error("分析任务执行失败，userId={}, fileId={}", userId, fileId, e);

                // 通知前端分析失败
                sseService.sendError(userId, e.getMessage());

                // 更新文件状态为失败
                dataInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_ERROR);
                fileInfoService.update(dataInfo);
            }
        });

    }

    /**
     * 处理分析结果
     * @param overAllState 图计算引擎返回的分析结果
     * @param dataInfo 文件信息
     */
    public void processAnalysisResult(OverAllState overAllState, DataInfo dataInfo) {
        Optional<Object> nodeResultObj = overAllState.value("nodeResult");

        NodeExecuteDto nodeExecuteDto = null;

        // 安全地转换对象类型
        if (nodeResultObj.isPresent() && nodeResultObj.get() instanceof NodeExecuteDto raw) {
            try {
                nodeExecuteDto = raw;
            } catch (Exception e) {
                log.error("Failed to convert nodeExecuteDtoList to List<NodeExecuteDto>", e);
            }
        }
        Long userId = dataInfo.getUserId();
        Long fileId = dataInfo.getId();

        // 封装结果
        if (nodeExecuteDto != null) {
            dataInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_PROCESSED);
            String vectorId = addVectorToRedis(userId, fileId, nodeExecuteDto);
            AnalysisResult analysisResult = buildAnalysisResult(vectorId, userId, fileId, nodeExecuteDto);
            analysisResultRepository.save(analysisResult);
            // 保存多维度标签（新版表）
            saveMultiDimensionTags(analysisResult.getId(), nodeExecuteDto.getMultiDimensionResult());
            // 保存推荐标签
            saveRecommendTags(analysisResult.getId(), userId, nodeExecuteDto.getMultiDimensionResult());

            // 异步触发标签关系发现
            final Long finalAnalysisResultId = analysisResult.getId();
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 等待事务提交
                    tagRelationDiscoveryService.discoverRelationsForAnalysis(finalAnalysisResultId);
                } catch (Exception e) {
                    log.warn("标签关系发现失败: {}", e.getMessage());
                }
            }).start();
        } else {
            dataInfo.setProcessedStatus(CommonConstant.FILE_PROCESS_STATUS_ERROR);
        }

        // 更新文件处理状态
        fileInfoService.update(dataInfo);

        log.info("分析结束");
    }

    /**
     * 添加到Redis向量数据库
     * @param executeDto 节点执行结果
     * @return 向量ID
     */
    private String addVectorToRedis(Long userId, Long fileId, NodeExecuteDto executeDto) {
        Map<String, Object> metaDataMap = new HashMap<>();
        metaDataMap.put("userId", userId);
        metaDataMap.put("fileId", fileId);

        String vectorId = UUID.randomUUID().toString();
        Document document = new Document(vectorId, executeDto.getProblemStatement(), metaDataMap);
        vectorStoreService.addOneAnalysisResult(document);
        return vectorId;
    }

    private AnalysisResult buildAnalysisResult(String vectorId, Long userId, Long fileId, NodeExecuteDto executeDto) {
        AnalysisResult analysisResult = new AnalysisResult();
        analysisResult.setVectorId(vectorId);
        analysisResult.setUserId(userId);
        analysisResult.setFileId(fileId);
        analysisResult.setProblemStatement(executeDto.getProblemStatement());
        analysisResult.setSolution(executeDto.getSolution());
        analysisResult.setStatus(executeDto.getStatus());
        analysisResult.setCreatedTime(LocalDateTime.now());

        return analysisResult;
    }

    /**
     * 保存多维度标签
     */
    private void saveMultiDimensionTags(Long analysisResultId, MultiDimensionTagResult multiResult) {
        if (multiResult == null) {
            return;
        }

        // 1. 技术领域标签（主标签）
        if (multiResult.getTechDomain() != null && multiResult.getTechDomain().getMainTagId() != null) {
            AnalysisTag mainTag = new AnalysisTag();
            mainTag.setAnalysisResultId(analysisResultId);
            mainTag.setTagId(multiResult.getTechDomain().getMainTagId());
            mainTag.setIsPrimary(true);
            mainTag.setConfidence(90);
            analysisTagRepository.save(mainTag);

            // 子标签
            if (multiResult.getTechDomain().getSubTagIds() != null) {
                for (int i = 0; i < multiResult.getTechDomain().getSubTagIds().size(); i++) {
                    AnalysisTag subTag = new AnalysisTag();
                    subTag.setAnalysisResultId(analysisResultId);
                    subTag.setTagId(multiResult.getTechDomain().getSubTagIds().get(i));
                    subTag.setIsPrimary(false);
                    subTag.setConfidence(85);
                    analysisTagRepository.save(subTag);
                }
            }
        }

        // 2. 思维范式标签
        if (multiResult.getThinkingParadigms() != null) {
            for (MultiDimensionTagResult.ThinkingParadigmResult paradigm : multiResult.getThinkingParadigms()) {
                if (paradigm.getTagId() != null) {
                    AnalysisTag paradigmTag = new AnalysisTag();
                    paradigmTag.setAnalysisResultId(analysisResultId);
                    paradigmTag.setTagId(paradigm.getTagId());
                    paradigmTag.setIsPrimary(false);
                    paradigmTag.setConfidence(paradigm.getConfidence() != null ? paradigm.getConfidence() : 80);
                    analysisTagRepository.save(paradigmTag);
                }
            }
        }

        // 3. 难度等级标签
        if (multiResult.getDifficulty() != null && multiResult.getDifficulty().getDifficultyId() != null) {
            AnalysisTag difficultyTag = new AnalysisTag();
            difficultyTag.setAnalysisResultId(analysisResultId);
            difficultyTag.setTagId(multiResult.getDifficulty().getDifficultyId());
            difficultyTag.setIsPrimary(false);
            difficultyTag.setConfidence(multiResult.getDifficulty().getConfidence() != null ?
                    multiResult.getDifficulty().getConfidence() : 75);
            analysisTagRepository.save(difficultyTag);
        }

        // 4. 应用场景标签
        if (multiResult.getScenario() != null && multiResult.getScenario().getScenarioId() != null) {
            AnalysisTag scenarioTag = new AnalysisTag();
            scenarioTag.setAnalysisResultId(analysisResultId);
            scenarioTag.setTagId(multiResult.getScenario().getScenarioId());
            scenarioTag.setIsPrimary(false);
            scenarioTag.setConfidence(multiResult.getScenario().getConfidence() != null ?
                    multiResult.getScenario().getConfidence() : 75);
            analysisTagRepository.save(scenarioTag);
        }
    }

    /**
     * 保存推荐标签
     * 将LLM建议但未匹配到现有标签的推荐词保存到推荐标签表
     */
    private void saveRecommendTags(Long analysisResultId, Long userId, MultiDimensionTagResult multiResult) {
        if (multiResult == null || multiResult.getTechDomain() == null) {
            return;
        }

        List<String> recommends = multiResult.getTechDomain().getRecommends();
        if (CollectionUtils.isEmpty(recommends)) {
            return;
        }

        // 获取该分析结果已关联的所有标签名称（用于去重判断）
        List<AnalysisTag> existingTags = analysisTagRepository.findByAnalysisResultId(analysisResultId);
        Set<String> existingTagNames = existingTags.stream()
                .map(AnalysisTag::getTag)
                .filter(tag -> tag != null)
                .map(Tag::getName)
                .collect(Collectors.toSet());

        // 过滤掉已存在的标签，只保存新的推荐
        for (String recommendName : recommends) {
            if (recommendName == null || recommendName.trim().isEmpty()) {
                continue;
            }

            String trimmedName = recommendName.trim();

            // 跳过已存在的标签
            if (existingTagNames.contains(trimmedName)) {
                continue;
            }

            // 检查是否已存在相同的推荐（避免重复）
            if (analysisRecommendTagRepository.existsByAnalysisResultIdAndTagName(analysisResultId, trimmedName)) {
                continue;
            }

            // 创建推荐标签记录
            AnalysisRecommendTag recommendTag = new AnalysisRecommendTag();
            recommendTag.setAnalysisResultId(analysisResultId);
            recommendTag.setTagName(trimmedName);
            recommendTag.setUserId(userId);
            recommendTag.setUserAction(AnalysisRecommendTag.UserAction.PENDING);

            try {
                analysisRecommendTagRepository.save(recommendTag);
                log.debug("保存推荐标签成功: analysisResultId={}, tagName={}", analysisResultId, trimmedName);
            } catch (Exception e) {
                log.warn("保存推荐标签失败: analysisResultId={}, tagName={}, error={}",
                        analysisResultId, trimmedName, e.getMessage());
            }
        }
    }

    public List<AnalysisTagVo> getTagList(Long userId) {
        List<AnalysisTagVo> resultList = new ArrayList<>();

        // 根据用户ID查询标签id到分析结果的映射
        List<AnalysisResult> analysisResultList = analysisResultRepository.findByUserId(userId);
        List<Long> analysisIdList = analysisResultList.stream().map(AnalysisResult::getId).toList();
        List<AnalysisTag> analysisTagList = analysisTagRepository.findByAnalysisResultIdIn(analysisIdList);

        // 过滤空标签
        analysisTagList = analysisTagList.stream().filter(item -> item.getTagId() != null).toList();

        List<Long> tagIdList = analysisTagList.stream()
                .filter(item -> item.getTagId() != null)
                .map(AnalysisTag::getTagId)
                .distinct()
                .toList();

        List<Tag> tagList = tagService.findByIdList(tagIdList);

        // 统计每个标签的出现次数
        Map<Long, Long> countMap = analysisTagList.stream()
                .collect(Collectors.groupingBy(AnalysisTag::getTagId, Collectors.counting()));

        // 构建标签ID到名称的映射
        Map<Long, String> tagIdToNameMap = tagList.stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));

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

        return resultList;
    }

    public List<AnalysisResultVo> page(Pageable pageable, AnalysisResultRequest resultRequest) {
        // 分页查询分析结果
        List<AnalysisResultInfo> page = analysisResultRepository.findByPage(pageable, resultRequest.getFileId(),
                resultRequest.getProblemStatement(), resultRequest.getTagId(), resultRequest.getUserId());

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
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }

        List<AnalysisResultVo> voList = list.stream().map(item -> {
            AnalysisResultVo vo = new AnalysisResultVo();
            vo.setId(item.getId());
            vo.setFileId(item.getFileId());
            vo.setProblemStatement(item.getProblemStatement());
            vo.setMainTagName(item.getTagName());
            vo.setFileName(item.getFileName().substring(0, item.getFileName().lastIndexOf(".")));
            vo.setCreateTime(item.getCreateTime());
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

    public List<SimilarAnalysisResultVo> getSimilarity(Long userId, Long analysisId) {
        AnalysisResult analysisResult = analysisResultRepository.findById(analysisId).orElse(null);
        if (analysisResult == null) {
            ExceptionUtils.throwDataNotFound("analysis result not found");
        }

        List<Document> documentList = vectorStoreService.searchSimilarityAnalysisResult(analysisResult.getProblemStatement(), userId);
        if (CollectionUtils.isEmpty(documentList)) {
            return Collections.emptyList();
        }
        // 构建相似分析结果VO列表
        return documentList.stream().map(document -> {
            SimilarAnalysisResultVo vo = new SimilarAnalysisResultVo();
            vo.setVectorId(document.getId());
            vo.setProblemStatement(document.getText());
            vo.setOriginContent(document.getMetadata().get("sessionContent").toString());
            vo.setScore((int) (document.getScore() * 100));
            return vo;
        }).toList();
    }
}
