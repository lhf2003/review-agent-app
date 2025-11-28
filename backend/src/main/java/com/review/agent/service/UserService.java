package com.review.agent.service;

import com.review.agent.common.utils.ObjectTransformUtil;
import com.review.agent.entity.UserInfo;
import com.review.agent.repository.UserInfoRepository;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.Date;

import static com.review.agent.common.UserConstant.SALT;

@Service
public class UserService {
    @Resource
    private UserInfoRepository userInfoRepository;

    /**
     * 注册用户
     * @param userInfo 用户信息
     */
    public void register(UserInfo userInfo) {
        String username = userInfo.getUsername();
        UserInfo userInfoFromDb = findByUsername(username);
        if (userInfoFromDb != null) {
            throw new IllegalArgumentException("username already exists");
        }
        String password = userInfo.getPassword();
        // 密码加密
        String handledPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        userInfo.setPassword(handledPassword);
        Date date = new Date();
        userInfo.setCreateTime(date);
        userInfo.setUpdateTime(date);
        userInfo.setAvatar("http://cdn.meetfei.cn/default-img/java-logo.png");

        userInfoRepository.save(userInfo);
    }

    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @return 用户信息
     */
    public UserInfo findByUsername(String username) {
        return userInfoRepository.findByUsername(username);
    }

    public UserInfo findById(Long id) {
        return userInfoRepository.findById(id).orElse(null);
    }

    public void updateInfo(UserInfo userInfo) {
        BeanUtils.copyProperties(userInfo, userInfo, ObjectTransformUtil.getNullPropertyNames(userInfo));
        userInfoRepository.save(userInfo);
    }
}