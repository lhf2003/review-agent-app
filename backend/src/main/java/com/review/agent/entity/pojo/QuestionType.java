package com.review.agent.entity.pojo;

/**
 * 题目类型枚举
 * 支持多种题型以适应不同的学习场景
 */
public enum QuestionType {

    /**
     * 单选题
     * 适用于：概念理解、基本原理、单一知识点考查
     */
    SINGLE_CHOICE("single_choice", "单选题"),

    /**
     * 多选题
     * 适用于：多要点问题、关联知识点、综合性概念
     */
    MULTIPLE_CHOICE("multiple_choice", "多选题"),

    /**
     * 判断题
     * 适用于：是非判断、概念辨析、常见误区
     */
    TRUE_FALSE("true_false", "判断题"),

    /**
     * 填空题
     * 适用于：关键术语、核心概念、参数记忆
     */
    FILL_BLANK("fill_blank", "填空题"),

    /**
     * 代码识别/调试题
     * 适用于：代码分析、错误排查、逻辑理解
     */
    CODE_SNIPPET("code_snippet", "代码识别题");

    private final String code;
    private final String displayName;

    QuestionType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 根据代码值获取枚举
     */
    public static QuestionType fromCode(String code) {
        for (QuestionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown question type code: " + code);
    }
}
