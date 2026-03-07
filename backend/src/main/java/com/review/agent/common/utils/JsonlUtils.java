package com.review.agent.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.review.agent.entity.dto.NodeExecuteDto;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.StringReader;
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

    /**
     * 解析 JSON Lines 格式内容为会话列表
     * @param content JSONL 内容
     * @param userId 用户ID
     * @param fileId 文件ID
     * @return 会话列表
     */
    public static List<NodeExecuteDto> parseJsonlContent(String content, Long userId, Long fileId) {
        List<NodeExecuteDto> result = new ArrayList<>();
        
        if (content == null || content.trim().isEmpty()) {
            return result;
        }

        StringBuilder sessionContent = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                try {
                    JSONObject record = JSON.parseObject(line);
                    String timestamp = record.getString("timestamp");
                    String request = record.getString("request");
                    String reply = record.getString("reply");
                    
                    // 构建会话内容（Markdown格式，兼容旧版分析逻辑）
                    sessionContent.append("# ").append(timestamp).append("\n");
                    sessionContent.append("## 用户请求\n");
                    sessionContent.append(request).append("\n");
                    sessionContent.append("## 模型回复\n");
                    sessionContent.append(reply).append("\n\n");
                    
                } catch (Exception e) {
                    log.warn("Failed to parse JSONL line: {}", line, e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to read JSONL content", e);
        }

        // 构建单个 NodeExecuteDto（一个JSONL文件 = 一个会话）
        if (sessionContent.length() > 0) {
            NodeExecuteDto dto = new NodeExecuteDto();
            dto.setUserId(userId);
            dto.setFileId(fileId);
            dto.setSessionStart(0);
            dto.setSessionEnd(content.length());
            dto.setSessionContent(sessionContent.toString());
            result.add(dto);
        }

        return result;
    }

    /**
     * 检测文件是否为 JSONL 格式（根据文件名扩展名）
     * @param fileName 文件名
     * @return 是否为 JSONL 文件
     */
    public static boolean isJsonlFile(String fileName) {
        return fileName != null && fileName.toLowerCase().endsWith(".jsonl");
    }
}
