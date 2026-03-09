package com.review.agent.controller;

import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.AnalysisRecommendTag;
import com.review.agent.entity.pojo.Tag;
import com.review.agent.entity.vo.RecommendTagVO;
import com.review.agent.service.TagRecommendService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推荐标签管理控制器
 * 处理AI分析生成的推荐标签的查询、采纳、忽略等操作
 */
@Slf4j
@RestController
@RequestMapping("/tag/recommendations")
public class TagRecommendController {

    @Resource
    private TagRecommendService tagRecommendService;

    @Resource
    private SecurityUtils securityUtils;

    /**
     * 获取当前用户的推荐标签列表
     * 返回按状态分组的结构：PENDING、ADOPTED、IGNORED（IGNORED只返回最近5条）
     *
     * @return 按状态分组的推荐标签Map
     */
    @GetMapping
    public ResponseEntity<Map<String, List<RecommendTagVO>>> getRecommendations() {
        Long userId = securityUtils.getCurrentUserId();
        log.debug("获取推荐标签列表, userId={}", userId);

        Map<String, List<RecommendTagVO>> recommendations = tagRecommendService.getRecommendationsGroupedByStatus(userId);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * 获取推荐标签统计信息
     *
     * @return 统计信息：待处理数量、已采纳数量、已忽略数量
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getRecommendationStats() {
        Long userId = securityUtils.getCurrentUserId();
        Map<String, Object> stats = tagRecommendService.getRecommendationStats(userId);
        return ResponseEntity.ok(stats);
    }

    /**
     * 采纳推荐标签
     * 将推荐标签创建为新标签，并关联到对应的技术领域
     *
     * @param id 推荐标签ID
     * @return 创建的新标签
     */
    @PostMapping("/{id}/adopt")
    public ResponseEntity<Tag> adoptRecommendation(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        log.info("采纳推荐标签, userId={}, recommendationId={}", userId, id);

        Tag newTag = tagRecommendService.adoptRecommendation(id, userId);
        return ResponseEntity.ok(newTag);
    }

    /**
     * 忽略推荐标签
     *
     * @param id 推荐标签ID
     * @return 操作结果
     */
    @PostMapping("/{id}/ignore")
    public ResponseEntity<Map<String, Object>> ignoreRecommendation(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        log.info("忽略推荐标签, userId={}, recommendationId={}", userId, id);

        boolean success = tagRecommendService.ignoreRecommendation(id, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "已忽略推荐标签" : "操作失败");
        return ResponseEntity.ok(result);
    }

    /**
     * 批量采纳推荐标签
     *
     * @param ids 推荐标签ID列表
     * @return 创建的新标签列表
     */
    @PostMapping("/adopt-all")
    public ResponseEntity<List<Tag>> adoptAllRecommendations(@RequestBody List<Long> ids) {
        Long userId = securityUtils.getCurrentUserId();
        log.info("批量采纳推荐标签, userId={}, count={}", userId, ids != null ? ids.size() : 0);

        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Tag> newTags = tagRecommendService.adoptAllRecommendations(ids, userId);
        return ResponseEntity.ok(newTags);
    }

    /**
     * 批量忽略推荐标签
     *
     * @param ids 推荐标签ID列表
     * @return 操作结果
     */
    @PostMapping("/ignore-all")
    public ResponseEntity<Map<String, Object>> ignoreAllRecommendations(@RequestBody List<Long> ids) {
        Long userId = securityUtils.getCurrentUserId();
        log.info("批量忽略推荐标签, userId={}, count={}", userId, ids != null ? ids.size() : 0);

        Map<String, Object> result = new HashMap<>();
        if (ids == null || ids.isEmpty()) {
            result.put("success", true);
            result.put("count", 0);
            return ResponseEntity.ok(result);
        }

        int count = tagRecommendService.ignoreAllRecommendations(ids, userId);
        result.put("success", count > 0);
        result.put("count", count);
        result.put("message", "已忽略 " + count + " 个推荐标签");
        return ResponseEntity.ok(result);
    }
}
