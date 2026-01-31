package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 用户默认模型配置实体
 * 用于存储用户为不同功能场景配置的默认模型
 */
@Getter
@Setter
@Entity
@Table(name = "user_default_model_config", schema = "review_agent")
public class UserDefaultModelConfig {
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 模型类型
     * SESSION_SPLIT(会话拆分), TAG_CLASSIFICATION(标签分类), SMART_ANALYSIS(智能分析)
     */
    @Column(name = "model_type", nullable = false, length = 50)
    private String modelType;

    /**
     * 模型提供商ID（关联 user_llm_config 表）
     */
    @Column(name = "provider_id")
    private Integer providerId;

    /**
     * 选中的模型名称
     */
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    /**
     * 创建时间
     */
    @Column(name = "created_time")
    private Date createdTime;

    /**
     * 更新时间
     */
    @Column(name = "updated_time")
    private Date updatedTime;
}
