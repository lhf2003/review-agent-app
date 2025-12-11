package com.review.agent.entity.projection;

import java.util.Date;

/**
 * Projection for {@link com.review.agent.entity.AnalysisResult}
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