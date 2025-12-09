package com.review.agent.service;

import com.review.agent.common.utils.ExceptionUtils;
import com.review.agent.common.utils.ObjectTransformUtil;
import com.review.agent.entity.UserConfig;
import com.review.agent.entity.UserInfo;
import com.review.agent.entity.request.updatePasswordRequest;
import com.review.agent.repository.UserConfigRepository;
import com.review.agent.repository.UserInfoRepository;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

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
            ExceptionUtils.throwDataInUse("user " + username + " already exists");
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
            ExceptionUtils.throwDataNotFound("username not found");
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
    @Transactional
    public void updateInfo(Long userId, UserInfo userInfo) {
        UserInfo existing = findById(userId);
        if (existing == null) {
            ExceptionUtils.throwDataNotFound("user not found");
        }
        BeanUtils.copyProperties(userInfo, existing, ObjectTransformUtil.getNullPropertyNames(userInfo));
        userInfoRepository.save(existing);
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

//    public List<Integer> getLlmProviderList() {
//        return List.of(UserConstant.LLM_PROVIDER_OPENAI, UserConstant.LLM_PROVIDER_TAG);
//    }

    /**
     * 更新用户配置信息
     * @param userConfig 用户配置信息
     */
    public void updateUserConfig(UserConfig userConfig) {
        // 校验用户是否存在
        UserConfig userConfigFromDb = getUserConfig(userConfig.getUserId());
        if (userConfigFromDb == null) {
            ExceptionUtils.throwDataNotFound("user config not found");
        }
        Integer interval = userConfig.getScanIntervalSeconds();
        if (interval != null) {
            if (interval < 3600 || interval > 43200) {
                ExceptionUtils.throwParamError("scan interval must be 3600-43200 seconds");
            }
        }
        BeanUtils.copyProperties(userConfig, userConfigFromDb, ObjectTransformUtil.getNullPropertyNames(userConfig));
        userConfigRepository.save(userConfigFromDb);
    }

    public void uploadAvatar(Long userId, MultipartFile avatar) {
        // 校验文件是否为空
        if (avatar.isEmpty()) {
            throw new IllegalArgumentException("avatar file is empty");
        }
    }

    public void updatePassword(Long userId, updatePasswordRequest request) {
        // 校验用户是否存在
        UserInfo userInfo = findById(userId);
        if (userInfo == null) {
            ExceptionUtils.throwDataNotFound("user not found");
        }
        // 校验旧密码是否正确
        if (!userInfo.getPassword().equals(DigestUtils.md5DigestAsHex((SALT + request.getOldPassword()).getBytes()))) {
            ExceptionUtils.throwPasswordError();
        }
        // 密码加密
        String handledPassword = DigestUtils.md5DigestAsHex((SALT + request.getNewPassword()).getBytes());
        userInfo.setPassword(handledPassword);
        userInfoRepository.save(userInfo);
    }
    // endregion
}
