package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐标签VO
 * 用于前端展示推荐标签信息
 */
@Data
public class RecommendTagVO {

    /**
     * 推荐标签ID
     */
    private Long id;

    /**
     * 关联的分析结果ID
     */
    private Long analysisResultId;

    /**
     * 推荐标签名称
     */
    private String tagName;

    /**
     * 用户操作状态：PENDING/ADOPTED/IGNORED
     */
    private String userAction;

    /**
     * 用户操作状态中文描述
     */
    private String userActionText;

    /**
     * 关联的分析结果问题描述
     */
    private String problemStatement;

    /**
     * 关联的文件名
     */
    private String fileName;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;

    /**
     * 根据状态获取中文描述
     */
    public static String getActionText(String action) {
        return switch (action) {
            case "PENDING" -> "待处理";
            case "ADOPTED" -> "已采纳";
            case "IGNORED" -> "已忽略";
            default -> "未知";
        };
    }
}
