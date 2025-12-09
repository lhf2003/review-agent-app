package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class AnalysisResultVo {
    private Long id;
    private Long fileId;
    private String vectorId;
    private String problemStatement;
    private String mainTagName;
    private List<String> subTagNameList;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}