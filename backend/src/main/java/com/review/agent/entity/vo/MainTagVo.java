package com.review.agent.entity.vo;

import com.review.agent.entity.SubTag;
import lombok.Data;

import java.util.List;

@Data
public class MainTagVo {
    private Long id;
    private String name;
    private List<SubTag> subTagList;
}