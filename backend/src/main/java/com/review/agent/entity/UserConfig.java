package com.review.agent.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "user_config", schema = "review_agent")
public class UserConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "user_id")
    private Long userId;

    @Size(max = 255)
    @Column(name = "scan_directory")
    private String scanDirectory;

    @ColumnDefault("1")
    @Column(name = "auto_scan_enabled")
    private Boolean autoScanEnabled;

    @ColumnDefault("30")
    @Column(name = "scan_interval_seconds")
    private Integer scanIntervalSeconds;

    @Size(max = 50)
    @ColumnDefault("'openai'")
    @Column(name = "llm_provider", length = 50)
    private String llmProvider;

    @Size(max = 255)
    @Column(name = "openai_api_key_encrypted")
    private String openaiApiKeyEncrypted;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "update_time")
    private Date updateTime;

}