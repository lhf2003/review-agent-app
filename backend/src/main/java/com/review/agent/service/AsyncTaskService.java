package com.review.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.common.sse.SseConnectionManager;
import com.review.agent.entity.pojo.AsyncTask;
import com.review.agent.repository.AsyncTaskRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 异步任务服务
 * 管理异步任务的创建、状态更新和结果通知
 */
@Service
@Slf4j
public class AsyncTaskService {

    @Resource
    private AsyncTaskRepository asyncTaskRepository;

    @Resource
    private SseConnectionManager connectionManager;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 创建异步任务
     *
     * @param userId   用户ID
     * @param taskType 任务类型
     * @return 创建的任务
     */
    @Transactional
    public AsyncTask createTask(Long userId, String taskType) {
        AsyncTask task = new AsyncTask();
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setStatus(AsyncTask.TaskStatus.PENDING);
        task.setProgress(0);

        AsyncTask saved = asyncTaskRepository.save(task);
        log.info("[Task-{}] 创建异步任务，类型: {}, 用户: {}", saved.getId(), taskType, userId);
        return saved;
    }

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 新状态
     */
    @Transactional
    public void updateStatus(Long taskId, AsyncTask.TaskStatus status) {
        AsyncTask task = asyncTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        task.setStatus(status);
        if (status == AsyncTask.TaskStatus.RUNNING) {
            task.setProgress(10);
        }

        asyncTaskRepository.save(task);
        log.debug("[Task-{}] 状态更新为: {}", taskId, status);
    }

    /**
     * 更新任务进度
     *
     * @param taskId   任务ID
     * @param progress 进度值 (0-100)
     */
    @Transactional
    public void updateProgress(Long taskId, int progress) {
        AsyncTask task = asyncTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        task.setProgress(Math.min(100, Math.max(0, progress)));
        asyncTaskRepository.save(task);

        // 发送进度通知
        connectionManager.sendEvent(task.getUserId(), "task_progress", String.format("任务进度: %d%%", progress));
    }

    /**
     * 完成任务
     *
     * @param taskId 任务ID
     * @param result 结果对象
     */
    @Transactional
    public void completeTask(Long taskId, Object result) {
        AsyncTask task = asyncTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        task.setStatus(AsyncTask.TaskStatus.COMPLETED);
        task.setProgress(100);
        task.setCompletedTime(LocalDateTime.now());

        try {
            task.setResult(objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            log.error("[Task-{}] 序列化结果失败: {}", taskId, e.getMessage());
            task.setResult("{\"error\":\"序列化失败\"}");
        }

        asyncTaskRepository.save(task);
        log.info("[Task-{}] 任务完成，用户: {}", taskId, task.getUserId());

        // SSE通知
        connectionManager.sendEvent(task.getUserId(), "task_completed",
                new TaskNotification(taskId, task.getTaskType(), result));
    }

    /**
     * 标记任务失败
     *
     * @param taskId       任务ID
     * @param errorMessage 错误信息
     */
    @Transactional
    public void failTask(Long taskId, String errorMessage) {
        AsyncTask task = asyncTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        task.setStatus(AsyncTask.TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        task.setCompletedTime(LocalDateTime.now());

        asyncTaskRepository.save(task);
        log.error("[Task-{}] 任务失败，用户: {}, 错误: {}", taskId, task.getUserId(), errorMessage);

        // SSE通知
        connectionManager.sendEvent(task.getUserId(), "task_failed",
                new TaskErrorNotification(taskId, task.getTaskType(), errorMessage));
    }

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 任务
     */
    public AsyncTask getTask(Long taskId, Long userId) {
        return asyncTaskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
    }

    // ========== 内部通知类 ==========

    /**
     * 任务完成通知
     */
    public record TaskNotification(Long taskId, String taskType, Object result) {}

    /**
     * 任务失败通知
     */
    public record TaskErrorNotification(Long taskId, String taskType, String error) {}
}
