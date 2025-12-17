package com.review.agent.entity.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StatisticRequest {
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
}