package com.review.agent.common.utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 用于生成、验证和解析 JWT Token
 *
 * @author Review Agent
 * @since 2026-01-31
 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:review-agent-secret-key-for-jwt-token-generation-and-verification-must-be-at-least-256-bits-long}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 默认 24 小时（毫秒）
    private Long expiration;

    /**
     * 生成 JWT Token
     *
     * @param userId 用户 ID
     * @return JWT Token 字符串
     */
    public String generateToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 Token 中获取用户 ID
     *
     * @param token JWT Token
     * @return 用户 ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 验证 Token 是否有效
     *
     * @param token JWT Token
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException e) {
            log.error("JWT 签名无效: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("JWT 格式无效: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT 已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT 不支持: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT 声明为空: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 检查 Token 是否即将过期（剩余时间小于 1 小时）
     *
     * @param token JWT Token
     * @return true-即将过期，false-未即将过期
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            return remainingTime < (60 * 60 * 1000); // 1 小时
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析 Token 获取 Claims
     *
     * @param token JWT Token
     * @return Claims
     * @throws JwtException 如果 Token 无效
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取签名密钥
     *
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
