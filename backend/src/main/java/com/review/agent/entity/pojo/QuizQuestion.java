package com.review.agent.entity.pojo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "quiz_question", schema = "review_agent")
public class QuizQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quiz_id", nullable = false)
    private Long quizId;

    @Column(name = "related_analysis_id")
    private Long relatedAnalysisId;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "options_json", nullable = false, columnDefinition = "JSON")
    private String optionsJson;

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "user_answer")
    private String userAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    // ========================================
    // 新增字段（支持题型多样化）
    // ========================================

    /**
     * 题目类型
     * 使用自定义转换器存储 code 值（如 "single_choice"）而不是枚举名称
     */
    @Convert(converter = QuestionTypeConverter.class)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType = QuestionType.SINGLE_CHOICE;

    /**
     * 难度等级 (1-非常简单, 2-简单, 3-中等, 4-困难, 5-非常困难)
     */
    @Column(name = "difficulty_level")
    private Integer difficultyLevel = 3;

    /**
     * 知识点标签（用于掌握度分析）
     */
    @Column(name = "knowledge_point", length = 100)
    private String knowledgePoint;

    /**
     * 答题时限（秒）
     */
    @Column(name = "time_limit")
    private Integer timeLimit = 60;

    /**
     * 被回答次数（用于题目质量评估）
     */
    @Column(name = "answer_count")
    private Integer answerCount = 0;

    /**
     * 正确次数（用于难度校准）
     */
    @Column(name = "correct_count")
    private Integer correctCount = 0;

    @Column(name = "created_time")
    private LocalDateTime createdTime;

    @PrePersist
    public void prePersist() {
        if (this.createdTime == null) this.createdTime = LocalDateTime.now();
        if (this.questionType == null) this.questionType = QuestionType.SINGLE_CHOICE;
        if (this.difficultyLevel == null) this.difficultyLevel = 3;
        if (this.timeLimit == null) this.timeLimit = 60;
        if (this.answerCount == null) this.answerCount = 0;
        if (this.correctCount == null) this.correctCount = 0;
    }
}
