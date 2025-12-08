package com.review.agent.entity.projection;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.review.agent.entity.DataInfo;

import java.util.Date;

/**
 * Projection for {@link DataInfo}
 */
public interface DataInfoVo {
    Long getId();

    Long getUserId();

    String getFileName();

    String getFileContent();

    Integer getProcessedStatus();

    Integer getSessionCount();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    Date getCreatedTime();

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    Date getUpdateTime();
}