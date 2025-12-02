package com.review.agent.entity.request;

import lombok.Data;

@Data
public class TagRequest {
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 标签名称
     */
    private String tagName;
    /**
     * 父标签ID 0表示主标签 非0表示子标签
     */
    private Integer parentId;

}