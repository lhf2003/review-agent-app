package com.review.agent.entity.pojo;

import com.review.agent.common.enums.QuestionType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * QuestionType 枚举转换器
 * 在数据库中存储 code 值（如 "single_choice"）而不是枚举名称（如 "SINGLE_CHOICE"）
 */
@Converter(autoApply = true)
public class QuestionTypeConverter implements AttributeConverter<QuestionType, String> {

    @Override
    public String convertToDatabaseColumn(QuestionType attribute) {
        if (attribute == null) {
            return null;
        }
        // 存储枚举的 code 值（如 "single_choice"）
        return attribute.getCode();
    }

    @Override
    public QuestionType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return QuestionType.SINGLE_CHOICE; // 默认值
        }
        // 从 code 值转换为枚举
        try {
            return QuestionType.fromCode(dbData);
        } catch (IllegalArgumentException e) {
            // 如果转换失败，返回默认值并记录错误
            return QuestionType.SINGLE_CHOICE;
        }
    }
}
