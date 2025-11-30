package com.review.agent.service;

import com.review.agent.common.constant.UserConstant;
import com.review.agent.common.utils.ObjectTransformUtil;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.UserConfig;
import com.review.agent.entity.UserInfo;
import com.review.agent.repository.UserConfigRepository;
import com.review.agent.repository.UserInfoRepository;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.List;

import static com.review.agent.common.constant.UserConstant.SALT;

@Service
public class UserService {
    @Resource
    private UserInfoRepository userInfoRepository;

    @Resource
    private UserConfigRepository userConfigRepository;

    // region 用户信息相关

    /**
     * 注册用户
     * @param userInfo 用户信息
     */
    @Transactional
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


        // 初始化用户配置
        UserConfig userConfig = new UserConfig();
        userConfig.setUserId(userInfo.getId());
        // windows用户默认扫描目录
        userConfig.setScanDirectory("C:\\Users\\" + username + "\\Desktop");
        userConfig.setAutoScanEnabled(true);
        userConfig.setScanIntervalSeconds(30);
        userConfig.setLlmProvider("openai");
        userConfig.setOpenaiApiKeyEncrypted("");
        userConfig.setUpdateTime(date);

        userConfigRepository.save(userConfig);
    }

    /**
     * 根据用户名查询用户信息
     * @param username 用户名
     * @return 用户信息
     */
    public UserInfo findByUsername(String username) {
        UserInfo userInfo = userInfoRepository.findByUsername(username);
        if (userInfo == null) {
            throw new IllegalArgumentException("username not found");
        }
        return userInfo;
    }

    public UserInfo findById(Long id) {
        return userInfoRepository.findById(id).orElse(null);
    }

    /**
     * 更新用户信息
     * @param userInfo 用户信息
     */
    public void updateInfo(UserInfo userInfo) {
        BeanUtils.copyProperties(userInfo, userInfo, ObjectTransformUtil.getNullPropertyNames(userInfo));
        userInfoRepository.save(userInfo);
    }

    // endregion

    // region 用户配置相关
    public List<UserConfig> findAllUserConfig() {
        return userConfigRepository.findAll();
    }

    /**
     * 获取用户配置信息
     * @param userId 用户ID
     * @return 用户配置信息
     */
    public UserConfig getUserConfig(Long userId) {
        return userConfigRepository.findByUserId(userId);
    }

    /**
     * 更新用户配置信息
     * @param userConfig 用户配置信息
     */
    public void updateUserConfig(UserConfig userConfig) {
        // 校验用户是否存在
        UserConfig userConfigFromDb = getUserConfig(userConfig.getUserId());
        if (userConfigFromDb == null) {
            throw new IllegalArgumentException("user config not found");
        }
        Integer interval = userConfig.getScanIntervalSeconds();
        if (interval != null) {
            if (interval < 3600 || interval > 43200) {
                throw new IllegalArgumentException("scan interval must be 3600-43200 seconds");
            }
        }
        BeanUtils.copyProperties(userConfig, userConfigFromDb, ObjectTransformUtil.getNullPropertyNames(userConfig));
        userConfigRepository.save(userConfigFromDb);
    }
    // endregion
}
