package com.review.agent.common.enums;

public enum ModelCapability {
    TOOL_CALLING,   // 支持函数调用/工具
    JSON_MODE,      // 支持强制JSON输出
    REASONING,      // 支持思维链 (CoT)
    VISION          // 支持图像识别
}