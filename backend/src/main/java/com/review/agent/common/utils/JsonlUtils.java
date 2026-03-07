package com.review.agent.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.review.agent.entity.dto.NodeExecuteDto;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON Lines 格式解析工具
 */
@Slf4j
public class JsonlUtils {

    /**
     * 检测内容是否为 JSON Lines 格式
     * @param content 文件内容
     * @return 是否为 JSONL 格式
     */
    public static boolean isJsonlFormat(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String firstLine = content.trim().split("\\r?\\n")[0];
        if (firstLine.isEmpty()) {
            return false;
        }
        
        try {
            JSONObject json = JSON.parseObject(firstLine);
            // JSONL 格式的特征：必须包含 timestamp, request, reply 字段
            return json.containsKey("timestamp") 
                && json.containsKey("request") 
                && json.containsKey("reply");
        } catch (Exception e) {
            return false;
        }
    }
}
