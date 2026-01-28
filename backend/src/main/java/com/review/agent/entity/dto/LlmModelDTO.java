package com.review.agent.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmModelDTO {
    private String id;
    private String object;
    private String ownedBy;
    private List<String> capabilities; // e.g., ["chat", "tools", "vision"]
}
