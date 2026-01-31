package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 成就定义表
 */
@Getter
@Setter
@Entity
@Table(name = "achievement_definition")
public class AchievementDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull(message = "成就代码不能为空")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @NotNull(message = "成就名称不能为空")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull(message = "成就描述不能为空")
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @NotNull(message = "成就图标不能为空")
    @Column(name = "icon", nullable = false, length = 50)
    private String icon;

    @NotNull(message = "条件类型不能为空")
    @Column(name = "condition_type", nullable = false, length = 50)
    private String conditionType;

    @NotNull(message = "条件值不能为空")
    @Column(name = "condition_value", nullable = false)
    private Integer conditionValue;

    @NotNull(message = "成就分类不能为空")
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @PrePersist
    public void prePersist() {
        if (this.createdTime == null) {
            this.createdTime = LocalDateTime.now();
        }
    }
}
