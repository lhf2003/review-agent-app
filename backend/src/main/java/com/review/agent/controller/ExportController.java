package com.review.agent.controller;

import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.AnalysisCollection;
import com.review.agent.repository.AnalysisCollectionRepository;
import com.review.agent.service.ExportService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 导出接口控制器
 * 提供知识卡片导出 Markdown 功能
 */
@Slf4j
@RestController
@RequestMapping("/export")
public class ExportController {

    @Resource
    private ExportService exportService;

    @Resource
    private SecurityUtils securityUtils;

    @Resource
    private AnalysisCollectionRepository analysisCollectionRepository;

    /**
     * 导出单个分析结果
     * @param id 分析结果ID
     * @return Markdown 文件
     */
    @GetMapping("/analysis/{id}")
    public ResponseEntity<byte[]> exportAnalysis(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        log.info("用户 {} 导出分析结果 {}", userId, id);

        byte[] content = exportService.exportAnalysisResultToMarkdown(id);
        String filename = exportService.generateAnalysisExportFilename(id);

        return buildFileResponse(content, filename);
    }

    /**
     * 批量导出分析结果
     * @param ids 分析结果ID列表
     * @return Markdown 文件
     */
    @PostMapping("/analysis/batch")
    public ResponseEntity<byte[]> exportBatchAnalysis(@RequestBody List<Long> ids) {
        Long userId = securityUtils.getCurrentUserId();
        log.info("用户 {} 批量导出 {} 条分析结果", userId, ids.size());

        byte[] content = exportService.exportBatchAnalysisResults(ids);
        String filename = exportService.generateBatchExportFilename();

        return buildFileResponse(content, filename);
    }

    /**
     * 导出整个合集
     * @param id 合集ID
     * @return Markdown 文件
     */
    @GetMapping("/collection/{id}")
    public ResponseEntity<byte[]> exportCollection(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        log.info("用户 {} 导出合集 {}", userId, id);

        // 验证合集归属
        AnalysisCollection collection = analysisCollectionRepository.findByIdAndUserId(userId, id);
        if (collection == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = exportService.exportCollectionToMarkdown(id);
        String filename = exportService.generateCollectionExportFilename(collection.getName());

        return buildFileResponse(content, filename);
    }

    /**
     * 构建文件下载响应
     */
    private ResponseEntity<byte[]> buildFileResponse(byte[] content, String filename) {
        // 对中文文件名进行 URL 编码
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .contentLength(content.length)
                .body(content);
    }
}
