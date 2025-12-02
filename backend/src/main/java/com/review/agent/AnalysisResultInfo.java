package com.review.agent;

import java.util.Date;

/**
 * Projection for {@link com.review.agent.entity.AnalysisResult}
 */
public interface AnalysisResultInfo {
    Long getId();

    Long getFileId();

    String getVectorId();

    Long getUserId();

    String getTagName();

    String getProblemStatement();

//    String getSolution();
//
//    Integer getStatus();
//
//    Date getCreatedTime();
}