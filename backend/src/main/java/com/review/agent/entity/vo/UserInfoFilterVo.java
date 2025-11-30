package com.review.agent.entity.vo;

import lombok.Data;

/**
 * 用户信息过滤VO
 */
@Data
public class UserInfoFilterVo {
    private Long id;
    /**
     * 用户名
     */
    private String username;
    private String avatar;
}