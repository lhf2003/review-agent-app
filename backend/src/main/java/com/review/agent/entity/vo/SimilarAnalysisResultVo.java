package com.review.agent.entity.vo;

import lombok.Data;

/**
 * 相似分析结果VO
 */
@Data
public class SimilarAnalysisResultVo {
    /**
     * 向量id
     */
    private String vectorId;
    /**
     * 相似问题
     */
    private String problemStatement;
    /**
     * 原始内容
     */
    private String originContent;
    /**
     * 相似度 (单位：%)
     */
    private Integer score;
}