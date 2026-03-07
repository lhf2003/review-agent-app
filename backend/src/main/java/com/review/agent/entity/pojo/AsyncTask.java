package com.review.agent.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异步任务实体
 * 用于管理长时间运行的异步任务，如标签关系发现
 */
@Entity
@Table(name = "async_task")
@Data
public class AsyncTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_type", nullable = false, length = 50)
    private String taskType;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;

    @Column
    private Integer progress = 0;

    @Column(columnDefinition = "TEXT")
    private String result;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_time", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @Column(name = "completed_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedTime;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,    // 等待执行
        RUNNING,    // 运行中
        COMPLETED,  // 已完成
        FAILED      // 执行失败
    }

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        if (status == null) {
            status = TaskStatus.PENDING;
        }
        if (progress == null) {
            progress = 0;
        }
    }
}
