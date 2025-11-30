package com.review.agent.entity.vo;

import lombok.Data;

/**
 * 分析标签VO：在文件页面展示标签分类
 */
@Data
public class AnalysisTagVo {
    private Long tagId;
    private String tagName;
    private Integer count;
}