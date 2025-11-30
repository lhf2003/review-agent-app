package com.review.agent.entity.request;

import lombok.Data;

@Data
public class TagRequest {
    private Long userId;
    private String tagName;
}