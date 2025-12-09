package com.review.agent.entity.projection;

import java.util.Date;

/**
 * Projection for {@link com.review.agent.entity.AnalysisResult}
 */
public interface AnalysisResultInfo {
    Long getId();

    Long getFileId();

    Long getUserId();

    String getTagName();

    String getSubTagIds();

    String getProblemStatement();

    Date getCreateTime();
}