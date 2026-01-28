package com.review.agent.entity.vo;

import com.review.agent.entity.pojo.AnalysisResult;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CollectionDetailVo extends CollectionVo {
    private List<AnalysisResult> analysisResults;
}
