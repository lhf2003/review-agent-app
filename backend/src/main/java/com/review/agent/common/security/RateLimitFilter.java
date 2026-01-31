package com.review.agent.common.security;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;

/**
 * API 限流过滤器
 * 基于 Redis 实现滑动窗口限流算法
 *
 * @author Review Agent
 * @since 2026-01-31
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${rate-limit.login-requests-per-minute:5}")
    private int loginRequestsPerMinute;

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    // 支持直接访问和前端代理两种路径
    private static final String LOGIN_ENDPOINT_DIRECT = "/user/login";
    private static final String LOGIN_ENDPOINT_PROXY = "/api/user/login";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String uri = request.getRequestURI();

        // 确定限流策略（支持两种路径格式）
        boolean isLoginEndpoint = LOGIN_ENDPOINT_DIRECT.equals(uri) || LOGIN_ENDPOINT_PROXY.equals(uri);
        int limit = isLoginEndpoint ? loginRequestsPerMinute : requestsPerMinute;

        // 获取限流键（基于 IP 和用户 ID）
        String key = getRateLimitKey(request, uri);

        // 检查限流
        if (!checkRateLimit(key, limit)) {
            sendRateLimitResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 获取限流键
     *
     * @param request HTTP 请求
     * @param uri 请求 URI
     * @return 限流键
     */
    private String getRateLimitKey(HttpServletRequest request, String uri) {
        // 优先使用用户 ID（如果已认证）
        String userId = request.getHeader("userId");
        if (userId != null) {
            return RATE_LIMIT_PREFIX + "user:" + userId + ":" + uri;
        }

        // 否则使用 IP 地址
        String ip = getClientIp(request);
        return RATE_LIMIT_PREFIX + "ip:" + ip + ":" + uri;
    }

    /**
     * 检查限流
     *
     * @param key 限流键
     * @param limit 限制数量
     * @return true-允许请求，false-超出限制
     */
    private boolean checkRateLimit(String key, int limit) {
        try {
            // 当前时间戳（秒）
            long now = System.currentTimeMillis() / 1000;

            // 移除窗口外的旧记录
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, now - 60);

            // 检查当前请求数
            Long count = redisTemplate.opsForZSet().count(key, now - 60, now);

            if (count == null || count < limit) {
                // 添加当前请求
                redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
                // 设置过期时间（2 分钟，保证窗口外数据能被清理）
                redisTemplate.expire(key, Duration.ofMinutes(2));
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("限流检查异常: {}", e.getMessage());
            // 限流检查失败时，允许请求通过（降级策略）
            return true;
        }
    }

    /**
     * 获取客户端 IP 地址
     *
     * @param request HTTP 请求
     * @return IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多个 IP 的情况（X-Forwarded-For 可能包含多个 IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 发送限流响应
     *
     * @param response HTTP 响应
     * @throws IOException IO 异常
     */
    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_URI_TOO_LONG);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        BaseResponse<Void> errorResponse = new BaseResponse(
                ErrorCode.TOO_MANY_REQUESTS.getCode(),
                ErrorCode.TOO_MANY_REQUESTS.getMessage()
        );

        response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
    }
}
