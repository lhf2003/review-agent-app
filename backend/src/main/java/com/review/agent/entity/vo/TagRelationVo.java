package com.review.agent.entity.vo;


import com.review.agent.entity.pojo.SubTag;
import lombok.Data;

import java.util.List;

/**
 * 标签关系VO
 */
@Data
public class TagRelationVo {
    private Long mainTagId;
    private String name;
    private List<SubTag> subTagList;
}