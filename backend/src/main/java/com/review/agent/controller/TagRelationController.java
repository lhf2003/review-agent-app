package com.review.agent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.AsyncTask;
import com.review.agent.entity.pojo.TagRelation;
import com.review.agent.entity.vo.TagRelationVO;
import com.review.agent.service.AsyncTaskService;
import com.review.agent.service.TagRelationDiscoveryService;
import com.review.agent.service.TagRelationService;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 标签关系接口
 */
@Slf4j
@RestController
@RequestMapping("/tag-relation")
public class TagRelationController {

    @Resource
    private TagRelationService tagRelationService;

    @Resource
    private TagRelationDiscoveryService tagRelationDiscoveryService;

    @Resource
    private SecurityUtils securityUtils;

    @Resource
    private AsyncTaskService asyncTaskService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 获取标签关系（统一接口）
     * @param tagId 标签ID
     * @param type 关系类型：all(所有关系), dependencies(前置依赖), next(推荐下一步), similar(相似标签), complementary(互补标签)
     */
    @GetMapping("/{tagId}")
    public BaseResponse<?> getTagRelations(
            @PathVariable Long tagId,
            @RequestParam(defaultValue = "all") String type) {
        return switch (type.toLowerCase()) {
            case "dependencies" -> ResultUtil.success(tagRelationService.getTagDependencies(tagId));
            case "next" -> ResultUtil.success(tagRelationService.getRecommendedNextTags(tagId));
            case "similar" -> ResultUtil.success(tagRelationService.getSimilarTags(tagId));
            case "complementary" -> ResultUtil.success(tagRelationService.getComplementaryTags(tagId));
            default -> ResultUtil.success(tagRelationService.getTagRelations(tagId));
        };
    }

    /**
     * 创建标签关系
     */
    @PostMapping("/create")
    public BaseResponse<TagRelation> createRelation(@RequestBody CreateRelationRequest request) {
        TagRelation relation = tagRelationService.createRelation(
                request.getSourceTagId(),
                request.getTargetTagId(),
                TagRelation.RelationType.valueOf(request.getRelationType()),
                request.getStrength(),
                request.getEvidence()
        );
        return ResultUtil.success(relation);
    }

    /**
     * 删除标签关系
     */
    @DeleteMapping("/delete/{relationId}")
    public BaseResponse<Void> deleteRelation(@PathVariable Long relationId) {
        tagRelationService.deleteRelation(relationId);
        return ResultUtil.success();
    }

    /**
     * 更新关系强度
     */
    @PostMapping("/update-strength/{relationId}")
    public BaseResponse<TagRelation> updateRelationStrength(
            @PathVariable Long relationId,
            @RequestParam Integer strength) {
        return ResultUtil.success(tagRelationService.updateRelationStrength(relationId, strength));
    }

    /**
     * 触发单个分析结果的标签关系发现
     */
    @PostMapping("/discover/{analysisResultId}")
    public BaseResponse<Void> discoverRelationsForAnalysis(@PathVariable Long analysisResultId) {
        tagRelationDiscoveryService.discoverRelationsForAnalysis(analysisResultId);
        return ResultUtil.success();
    }

    /**
     * 批量发现用户的标签关系（基于共现）- 异步执行
     */
    @PostMapping("/discover-co-occurrence")
    public BaseResponse<TaskVO> discoverRelationsByCoOccurrence() {
        // 1. 在同步线程中获取 userId
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResultUtil.error("用户未登录");
        }

        // 2. 创建异步任务
        AsyncTask task = asyncTaskService.createTask(userId, "RELATION_DISCOVERY");

        // 3. 调用异步服务（不等待结果）
        tagRelationDiscoveryService.discoverRelationsByCoOccurrenceAsync(userId, task.getId());

        // 4. 立即返回任务信息
        TaskVO taskVO = new TaskVO(task.getId(), "PENDING", "标签关系发现任务已启动");
        return ResultUtil.success(taskVO);
    }

    /**
     * 查询任务状态
     */
    @GetMapping("/discover-task/{taskId}")
    public BaseResponse<TaskVO> getDiscoveryTaskStatus(@PathVariable Long taskId) {
        Long userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return ResultUtil.error("用户未登录");
        }

        AsyncTask task = asyncTaskService.getTask(taskId, userId);

        TaskVO vo = new TaskVO();
        vo.setTaskId(task.getId());
        vo.setStatus(task.getStatus().name());
        vo.setMessage(getStatusMessage(task.getStatus()));
        vo.setProgress(task.getProgress());

        if (task.getStatus() == AsyncTask.TaskStatus.COMPLETED && task.getResult() != null) {
            // 解析结果
            try {
                vo.setResult(objectMapper.readValue(task.getResult(), DiscoveryResult.class));
            } catch (Exception e) {
                log.error("解析任务结果失败: {}", e.getMessage());
                vo.setResult(null);
            }
        }

        if (task.getStatus() == AsyncTask.TaskStatus.FAILED) {
            vo.setError(task.getErrorMessage());
        }

        return ResultUtil.success(vo);
    }

    private String getStatusMessage(AsyncTask.TaskStatus status) {
        return switch (status) {
            case PENDING -> "任务等待执行";
            case RUNNING -> "正在分析标签共现关系...";
            case COMPLETED -> "关系发现完成";
            case FAILED -> "任务执行失败";
        };
    }

    // ========== 请求DTO ==========

    public static class CreateRelationRequest {
        private Long sourceTagId;
        private Long targetTagId;
        private String relationType;
        private Integer strength;
        private String evidence;

        // Getters and Setters
        public Long getSourceTagId() { return sourceTagId; }
        public void setSourceTagId(Long sourceTagId) { this.sourceTagId = sourceTagId; }
        public Long getTargetTagId() { return targetTagId; }
        public void setTargetTagId(Long targetTagId) { this.targetTagId = targetTagId; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
        public Integer getStrength() { return strength; }
        public void setStrength(Integer strength) { this.strength = strength; }
        public String getEvidence() { return evidence; }
        public void setEvidence(String evidence) { this.evidence = evidence; }
    }

    // ========== 响应VO ==========

    @Data
    public static class TaskVO {
        private Long taskId;
        private String status;
        private String message;
        private Integer progress;
        private Object result;
        private String error;

        public TaskVO() {}

        public TaskVO(Long taskId, String status, String message) {
            this.taskId = taskId;
            this.status = status;
            this.message = message;
        }
    }

    /**
     * 关系发现结果
     */
    public record DiscoveryResult(Long taskId, int foundCount, String status) {}
}
