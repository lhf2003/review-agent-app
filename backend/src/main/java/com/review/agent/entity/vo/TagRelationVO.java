package com.review.agent.entity.vo;

import com.review.agent.entity.pojo.TagRelation;
import lombok.Data;

/**
 * 标签关系VO
 */
@Data
public class TagRelationVO {
    private Long id;
    private Long sourceTagId;
    private String sourceTagName;
    private Long targetTagId;
    private String targetTagName;
    private TagRelation.RelationType relationType;
    private String relationTypeLabel;
    private Integer strength;
    private String evidence;
    private Boolean autoDetected;
    private String direction; // outgoing: 作为源标签, incoming: 作为目标标签
}
