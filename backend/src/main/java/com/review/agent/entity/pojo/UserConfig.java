package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

    @Column(name = "scan_directory")
    private String scanDirectory;

    @Column(name = "auto_scan_enabled", columnDefinition = "TINYINT")
    private Boolean autoScanEnabled;

    @Column(name = "scan_interval_seconds")
    private Integer scanIntervalSeconds;

    @Column(name = "update_time")
    private Date updateTime;

    /**
     * 是否启用每日分析
     */
    @Column(name = "daily_enabled", columnDefinition = "TINYINT")
    private Boolean dailyEnabled;

    /**
     * 日报的cron
     */
    @Column(name = "daily_cron")
    private String dailyCron;

    /**
     * 是否启用每周分析
     */
    @Column(name = "weekly_enabled", columnDefinition = "TINYINT")
    private Boolean weeklyEnabled;

    /**
     * 周报的cron
     */
    @Column(name = "weekly_cron")
    private String weeklyCron;

    /**
     * 是否启用自动分析
     */
    @Column(name = "auto_analysis_enabled", columnDefinition = "TINYINT")
    private Boolean autoAnalysisEnabled;

    /**
     * 自动分析间隔（分钟）
     */
    @Column(name = "analysis_interval_minutes")
    private Integer analysisIntervalMinutes;

}