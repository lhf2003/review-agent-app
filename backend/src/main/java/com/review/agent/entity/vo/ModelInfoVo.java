package com.review.agent.entity.vo;

import com.review.agent.common.enums.ModelCapability;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 模型列表
 */
@Data
@AllArgsConstructor
public class ModelInfoVo {
    private String id;          // 模型ID (deepseek-chat)
    private String name;        // 显示名称 (DeepSeek V3)
    private List<ModelCapability> capabilities; // 能力列表
}