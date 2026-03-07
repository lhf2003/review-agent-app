package com.review.agent.repository;

import com.review.agent.entity.pojo.AsyncTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 异步任务数据访问层
 */
@Repository
public interface AsyncTaskRepository extends JpaRepository<AsyncTask, Long> {

    /**
     * 根据用户ID查询任务列表，按创建时间倒序
     */
    List<AsyncTask> findByUserIdOrderByCreatedTimeDesc(Long userId);

    /**
     * 根据用户ID和状态查询任务
     */
    List<AsyncTask> findByUserIdAndStatus(Long userId, AsyncTask.TaskStatus status);

    /**
     * 根据ID和用户ID查询任务（确保用户只能访问自己的任务）
     */
    Optional<AsyncTask> findByIdAndUserId(Long id, Long userId);
}
