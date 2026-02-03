package com.review.agent.entity.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 题库版本检测结果
 * 用于检测合集内容更新后，题库是否需要重新生成
 */
@Data
@Builder
public class QuizVersionCheckResult {
    /**
     * 是否存在历史题库记录
     */
    private boolean hasExistingQuiz;

    /**
     * 历史题库ID（如果存在）
     */
    private Long quizId;

    /**
     * 是否需要创建新题库
     */
    private boolean needsCreation;

    /**
     * 版本是否匹配
     */
    private boolean isVersionMatch;

    /**
     * 题库是否已标记为过期
     */
    private boolean isOutdated;

    /**
     * 当前合集内容的哈希值
     */
    private String currentHash;

    /**
     * 数据库中存储的哈希值
     */
    private String storedHash;
}
