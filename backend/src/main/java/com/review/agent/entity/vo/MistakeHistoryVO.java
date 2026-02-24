package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 错题答题历史VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MistakeHistoryVO {

    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 错题记录ID
     */
    private Long mistakeId;

    /**
     * 错误答案
     */
    private String wrongAnswer;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 答题用时（秒）
     */
    private Integer timeSpent;

    /**
     * 是否正确
     */
    private Boolean isCorrect;

    /**
     * 答题时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private LocalDateTime createdTime;
}
