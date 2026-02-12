package com.review.agent.service;

import com.review.agent.common.utils.ExceptionUtils;
import com.review.agent.entity.pojo.*;
import com.review.agent.repository.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 导出服务
 * 负责将分析结果导出为 Markdown 格式
 */
@Slf4j
@Service
public class ExportService {

    @Resource
    private AnalysisResultRepository analysisResultRepository;

    @Resource
    private AnalysisTagRepository analysisTagRepository;

    @Resource
    private DataInfoRepository dataInfoRepository;

    @Resource
    private MainTagRepository mainTagRepository;

    @Resource
    private SubTagRepository subTagRepository;

    @Resource
    private AnalysisCollectionRepository analysisCollectionRepository;

    @Resource
    private CollectionRelationRepository collectionRelationRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 导出单个分析结果为 Markdown
     * @param analysisId 分析结果ID
     * @return Markdown 字节数组
     */
    public byte[] exportAnalysisResultToMarkdown(Long analysisId) {
        AnalysisResult result = analysisResultRepository.findById(analysisId)
                .orElseThrow(() -> {
                    ExceptionUtils.throwDataNotFound("分析结果不存在");
                    return new RuntimeException(); // 不会执行，仅满足编译器要求
                });

        // 获取标签信息
        AnalysisTag analysisTag = analysisTagRepository.findByAnalysisIdIn(List.of(analysisId))
                .stream()
                .findFirst()
                .orElse(null);

        // 获取文件信息
        DataInfo dataInfo = dataInfoRepository.findById(result.getFileId()).orElse(null);

        // 构建标签名称列表
        List<String> tagNames = buildTagNames(analysisTag);

        // 生成 Markdown
        String markdown = buildAnalysisMarkdown(result, dataInfo, tagNames);

        return markdown.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 批量导出分析结果为 Markdown
     * @param analysisIds 分析结果ID列表
     * @return Markdown 字节数组
     */
    public byte[] exportBatchAnalysisResults(List<Long> analysisIds) {
        if (CollectionUtils.isEmpty(analysisIds)) {
            throw new IllegalArgumentException("分析结果ID列表不能为空");
        }

        List<AnalysisResult> results = analysisResultRepository.findAllById(analysisIds);
        if (CollectionUtils.isEmpty(results)) {
            throw new IllegalArgumentException("未找到任何分析结果");
        }

        // 批量获取标签信息
        List<AnalysisTag> analysisTags = analysisTagRepository.findByAnalysisIdIn(analysisIds);
        Map<Long, AnalysisTag> tagMap = analysisTags.stream()
                .collect(Collectors.toMap(AnalysisTag::getAnalysisId, tag -> tag, (a, b) -> a));

        // 批量获取文件信息
        List<Long> fileIds = results.stream().map(AnalysisResult::getFileId).distinct().toList();
        List<DataInfo> dataInfos = dataInfoRepository.findAllById(fileIds);
        Map<Long, DataInfo> dataInfoMap = dataInfos.stream()
                .collect(Collectors.toMap(DataInfo::getId, info -> info, (a, b) -> a));

        // 构建批量导出的 Markdown
        StringBuilder sb = new StringBuilder();
        sb.append("# 知识卡片导出\n\n");
        sb.append("**导出时间**: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append("\n");
        sb.append("**共计**: ").append(results.size()).append(" 条记录\n\n");
        sb.append("---\n\n");

        for (int i = 0; i < results.size(); i++) {
            AnalysisResult result = results.get(i);
            AnalysisTag tag = tagMap.get(result.getId());
            DataInfo dataInfo = dataInfoMap.get(result.getFileId());
            List<String> tagNames = buildTagNames(tag);

            sb.append(buildAnalysisMarkdown(result, dataInfo, tagNames));

            if (i < results.size() - 1) {
                sb.append("\n---\n\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 导出整个合集为 Markdown
     * @param collectionId 合集ID
     * @return Markdown 字节数组
     */
    public byte[] exportCollectionToMarkdown(Long collectionId) {
        AnalysisCollection collection = analysisCollectionRepository.findById(collectionId)
                .orElseThrow(() -> {
                    ExceptionUtils.throwDataNotFound("合集不存在");
                    return new RuntimeException(); // 不会执行，仅满足编译器要求
                });

        // 获取合集关联的分析结果ID
        List<CollectionRelation> relations = collectionRelationRepository.findByCollectionId(collectionId);
        if (CollectionUtils.isEmpty(relations)) {
            // 空合集
            StringBuilder sb = new StringBuilder();
            sb.append("# ").append(escapeMarkdown(collection.getName())).append("\n\n");
            sb.append("**描述**: ").append(collection.getDescription() != null ? escapeMarkdown(collection.getDescription()) : "暂无描述").append("\n\n");
            sb.append("*此合集暂无收录内容*\n");
            return sb.toString().getBytes(StandardCharsets.UTF_8);
        }

        List<Long> analysisIds = relations.stream()
                .map(CollectionRelation::getAnalysisResultId)
                .toList();

        // 获取分析结果
        List<AnalysisResult> results = analysisResultRepository.findAllById(analysisIds);

        // 批量获取标签信息
        List<AnalysisTag> analysisTags = analysisTagRepository.findByAnalysisIdIn(analysisIds);
        Map<Long, AnalysisTag> tagMap = analysisTags.stream()
                .collect(Collectors.toMap(AnalysisTag::getAnalysisId, tag -> tag, (a, b) -> a));

        // 批量获取文件信息
        List<Long> fileIds = results.stream().map(AnalysisResult::getFileId).distinct().toList();
        List<DataInfo> dataInfos = dataInfoRepository.findAllById(fileIds);
        Map<Long, DataInfo> dataInfoMap = dataInfos.stream()
                .collect(Collectors.toMap(DataInfo::getId, info -> info, (a, b) -> a));

        // 构建合集导出的 Markdown
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(escapeMarkdown(collection.getName())).append("\n\n");
        sb.append("**描述**: ").append(collection.getDescription() != null ? escapeMarkdown(collection.getDescription()) : "暂无描述").append("\n\n");
        sb.append("**创建时间**: ").append(collection.getCreatedTime() != null
                ? collection.getCreatedTime().format(DATE_FORMATTER) : "未知").append("\n");
        sb.append("**共计**: ").append(results.size()).append(" 条记录\n\n");
        sb.append("---\n\n");

        for (int i = 0; i < results.size(); i++) {
            AnalysisResult result = results.get(i);
            AnalysisTag tag = tagMap.get(result.getId());
            DataInfo dataInfo = dataInfoMap.get(result.getFileId());
            List<String> tagNames = buildTagNames(tag);

            sb.append(buildAnalysisMarkdown(result, dataInfo, tagNames));

            if (i < results.size() - 1) {
                sb.append("\n---\n\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 构建单个分析结果的 Markdown
     */
    private String buildAnalysisMarkdown(AnalysisResult result, DataInfo dataInfo, List<String> tagNames) {
        StringBuilder sb = new StringBuilder();

        // 标题（使用问题陈述的第一行或截断）
        String title = extractTitle(result.getProblemStatement());
        sb.append("## ").append(escapeMarkdown(title)).append("\n\n");

        // 元信息
        sb.append("**标签**: ").append(tagNames.isEmpty() ? "无" : String.join(", ", tagNames)).append("\n");
        if (dataInfo != null) {
            sb.append("**来源文件**: ").append(escapeMarkdown(dataInfo.getFileName())).append("\n");
        }
        sb.append("**创建时间**: ").append(result.getCreatedTime() != null
                ? result.getCreatedTime().format(DATE_FORMATTER) : "未知").append("\n\n");

        // 问题描述
        sb.append("### 问题描述\n\n");
        sb.append(escapeMarkdown(result.getProblemStatement() != null
                ? result.getProblemStatement() : "暂无问题描述")).append("\n\n");

        // 解决方案
        sb.append("### 解决方案\n\n");
        sb.append(escapeMarkdown(result.getSolution() != null
                ? result.getSolution() : "暂无解决方案")).append("\n\n");

        return sb.toString();
    }

    /**
     * 构建标签名称列表
     */
    private List<String> buildTagNames(AnalysisTag analysisTag) {
        List<String> tagNames = new ArrayList<>();

        if (analysisTag == null) {
            return tagNames;
        }

        // 主标签
        if (analysisTag.getTagId() != null) {
            mainTagRepository.findById(analysisTag.getTagId())
                    .ifPresent(tag -> tagNames.add(tag.getName()));
        }

        // 子标签
        if (StringUtils.hasText(analysisTag.getSubTagId())) {
            List<Long> subTagIds = Arrays.stream(analysisTag.getSubTagId().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(Long::parseLong)
                    .toList();

            if (!subTagIds.isEmpty()) {
                List<SubTag> subTags = subTagRepository.findAllById(subTagIds);
                tagNames.addAll(subTags.stream().map(SubTag::getName).toList());
            }
        }

        return tagNames;
    }

    /**
     * 从问题陈述中提取标题（取第一行或截断前50字符）
     */
    private String extractTitle(String problemStatement) {
        if (!StringUtils.hasText(problemStatement)) {
            return "未命名";
        }

        // 取第一行
        int newlineIndex = problemStatement.indexOf('\n');
        String firstLine = newlineIndex > 0
                ? problemStatement.substring(0, newlineIndex).trim()
                : problemStatement.trim();

        // 截断过长的标题
        if (firstLine.length() > 80) {
            return firstLine.substring(0, 77) + "...";
        }

        return firstLine;
    }

    /**
     * 转义 Markdown 特殊字符
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        // 转义常见的 Markdown 特殊字符
        return text
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace(".", "\\.")
                .replace("!", "\\!")
                .replace("|", "\\|");
    }

    /**
     * 生成导出文件名
     */
    public String generateAnalysisExportFilename(Long analysisId) {
        return "analysis_" + analysisId + ".md";
    }

    /**
     * 生成批量导出文件名
     */
    public String generateBatchExportFilename() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "knowledge_export_" + dateStr + ".md";
    }

    /**
     * 生成合集导出文件名
     */
    public String generateCollectionExportFilename(String collectionName) {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        // 清理文件名中的非法字符
        String safeName = collectionName.replaceAll("[\\\\/:*?\"<>|]", "_");
        return "collection_" + safeName + "_" + dateStr + ".md";
    }
}
