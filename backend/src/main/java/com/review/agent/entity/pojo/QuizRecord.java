package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "quiz_record", schema = "review_agent")
public class QuizRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "collection_id", nullable = false)
    private Long collectionId;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "status", columnDefinition = "TINYINT")
    private Integer status; // 0=InProgress, 1=Completed

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    /**
     * 做题时间（用户实际提交答案的时间）
     * 区别于 created_time（记录创建时间）
     * 用于学习数据统计和趋势分析
     */
    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @PrePersist
    public void prePersist() {
        if (this.createdTime == null) this.createdTime = LocalDateTime.now();
        if (this.updatedTime == null) this.updatedTime = LocalDateTime.now();
    }

    /**
     * 删除标记（软删除）
     * false-未删除，true-已删除
     */
    @ColumnDefault("false")
    @Column(name = "deleted")
    private Boolean deleted = false;

    /**
     * 删除时间（软删除）
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * 合集内容哈希值（用于版本检测）
     */
    @Column(name = "collection_version_hash", length = 64)
    private String collectionVersionHash;

    /**
     * 生成题库时使用的分析结果ID列表（JSON格式）
     */
    @Column(name = "analysis_result_ids", columnDefinition = "TEXT")
    private String analysisResultIds;

    /**
     * 题型是否已过期（合集有新内容）
     */
    @Column(name = "is_outdated")
    private Boolean isOutdated = false;
}
