package com.review.agent.entity.request;

import lombok.Data;
import java.util.List;

@Data
public class CollectionItemRequest {
    private String action; // ADD, REMOVE
    private List<Long> analysisIds;
}
