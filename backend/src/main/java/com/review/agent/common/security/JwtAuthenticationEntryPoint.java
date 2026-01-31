package com.review.agent.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * JWT 认证入口点
 * 处理未认证的请求，返回 401 未授权响应
 *
 * @author Review Agent
 * @since 2026-01-31
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        log.warn("未授权访问: URI = {}, 原因 = {}", request.getRequestURI(), authException.getMessage());

        // 设置响应状态和内容类型
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 构造错误响应
        BaseResponse<Void> errorResponse = new BaseResponse(
                ErrorCode.UNAUTHORIZED.getCode(),
                ErrorCode.UNAUTHORIZED.getMessage()
        );

        // 写入响应
        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }
}
