package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学习数据概览统计 VO
 * 用于个人中心"数据概览"tab 展示核心统计指标
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学习数据概览统计")
public class OverviewStatsVO {

    @Schema(description = "总学习天数")
    private Integer totalDays;

    @Schema(description = "总测验次数")
    private Integer totalQuizzes;

    @Schema(description = "平均分数")
    private Double avgScore;

    @Schema(description = "待复习错题数")
    private Integer totalMistakes;

    @Schema(description = "总学习时长（分钟）")
    private Integer totalStudyMinutes;
}
