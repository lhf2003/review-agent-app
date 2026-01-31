package com.review.agent.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 VO
 * 包含 JWT Token 和用户信息
 *
 * @author Review Agent
 * @since 2026-01-31
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseVo {

    /**
     * JWT 访问令牌
     */
    private String token;

    /**
     * Token 类型（固定为 "Bearer"）
     */
    private String tokenType;

    /**
     * 用户信息
     */
    private UserInfoFilterVo userInfo;

    /**
     * Token 过期时间（毫秒时间戳）
     */
    private Long expiresIn;
}
