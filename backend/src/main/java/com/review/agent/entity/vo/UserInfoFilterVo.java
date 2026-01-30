package com.review.agent.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

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
    private String nickname;
    private String email;
    private String phone;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}