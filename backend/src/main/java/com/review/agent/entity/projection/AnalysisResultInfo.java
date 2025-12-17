package com.review.agent.entity.projection;

import com.review.agent.entity.pojo.AnalysisResult;

import java.util.Date;

/**
 * Projection for {@link AnalysisResult}
 */
public interface AnalysisResultInfo {
    Long getId();

    Long getFileId();

    String getFileName();

    Long getUserId();

    String getTagName();

    String getSubTagIds();

    String getProblemStatement();

    String getRecommendTag();

    Date getCreateTime();
}