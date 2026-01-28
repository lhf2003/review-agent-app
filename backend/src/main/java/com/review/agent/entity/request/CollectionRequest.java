package com.review.agent.entity.request;

import lombok.Data;
import java.util.List;

@Data
public class CollectionRequest {
    private String name;
    private String description;
    private List<Long> analysisIds;
}
