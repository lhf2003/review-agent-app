package com.review.agent.common.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Security 工具类
 * 用于从 SecurityContext 中获取当前用户信息
 *
 * @author Review Agent
 * @since 2026-01-31
 */
@Component
public class SecurityUtils {

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID，如果未认证则返回 null
     */
    public  Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return null;
    }

    /**
     * 检查当前用户是否已认证
     *
     * @return true-已认证，false-未认证
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
