package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "analysis_collection", schema = "review_agent")
public class AnalysisCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

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
    @ColumnDefault("0")
    @Column(name = "deleted")
    private Boolean deleted = false;

    /**
     * 删除时间（软删除）
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}