package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.AesUtil;
import com.review.agent.common.utils.JwtUtil;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.SelectedModel;
import com.review.agent.entity.pojo.UserConfig;
import com.review.agent.entity.pojo.UserInfo;
import com.review.agent.entity.pojo.UserLlmConfig;
import com.review.agent.entity.pojo.UserDefaultModelConfig;
import com.review.agent.entity.request.BasicConfigUpdateRequest;
import com.review.agent.entity.request.UpdatePasswordRequest;
import com.review.agent.entity.vo.LoginResponseVo;
import com.review.agent.entity.vo.UserInfoFilterVo;
import com.review.agent.entity.vo.UserStatsVo;
import com.review.agent.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


/**
 * 用户信息接口
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private SecurityUtils securityUtils;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    // region 用户信息接口

    /**
     * 注册用户
     */
    @PostMapping("/register")
    public BaseResponse<String> register(@RequestBody UserInfo userInfo) {
        // 解密前端传来的密码
        userInfo.setPassword(AesUtil.decrypt(userInfo.getPassword()));
        userService.register(userInfo);
        return ResultUtil.success("register success");
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginResponseVo> login(@RequestBody UserInfo userInfo) {
        // 解密前端传来的密码
        String plainPassword = AesUtil.decrypt(userInfo.getPassword());

        // 校验用户名密码是否正确
        UserInfo userInfoFromDb = userService.findByUsername(userInfo.getUsername());
        if (userInfoFromDb == null) {
            return ResultUtil.error("username not found");
        }

        // 使用 BCrypt 验证密码
        if (!passwordEncoder.matches(plainPassword, userInfoFromDb.getPassword())) {
            return ResultUtil.error("password not match");
        }

        // 生成 JWT Token
        String token = jwtUtil.generateToken(userInfoFromDb.getId());

        // 构造用户信息
        UserInfoFilterVo userInfoFilterVo = new UserInfoFilterVo();
        BeanUtils.copyProperties(userInfoFromDb, userInfoFilterVo);

        // 构造登录响应
        LoginResponseVo response = LoginResponseVo.builder()
                .token(token)
                .tokenType("Bearer")
                .userInfo(userInfoFilterVo)
                .expiresIn(System.currentTimeMillis() + jwtExpiration)
                .build();

        return ResultUtil.success(response);
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public BaseResponse<?> getUserInfo(@RequestParam("id") Long id) {
        UserInfo userInfo = userService.findById(id);
        if (userInfo == null) {
            return ResultUtil.error("user id not found");
        }
        return ResultUtil.success(userInfo);
    }

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/info")
    public BaseResponse<UserInfoFilterVo> getUserInfoByUsername() {
        Long userId = securityUtils.getCurrentUserId();
        UserInfo userInfo = userService.findById(userId);
        if (userInfo == null) {
            return ResultUtil.error("userId not found");
        }
        UserInfoFilterVo userInfoFilterVo = new UserInfoFilterVo();
        BeanUtils.copyProperties(userInfo, userInfoFilterVo);
        return ResultUtil.success(userInfoFilterVo);
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/info/upload/avatar")
    public BaseResponse<?> uploadAvatar(@RequestParam("avatar") MultipartFile avatar) {
        Long userId = securityUtils.getCurrentUserId();
        userService.uploadAvatar(userId, avatar);
        return ResultUtil.success("upload success");
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/info/update")
    public BaseResponse<?> updateUserInfo(@RequestBody UserInfo userInfo) {
        Long userId = securityUtils.getCurrentUserId();
        userService.updateInfo(userId, userInfo);
        return ResultUtil.success();
    }

    /**
     * 更新用户密码
     */
    @PostMapping("/info/update/password")
    public BaseResponse<?> updateUserPassword(@RequestBody UpdatePasswordRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        request.setOldPassword(AesUtil.decrypt(request.getOldPassword()));
        request.setNewPassword(AesUtil.decrypt(request.getNewPassword()));
        userService.updatePassword(userId, request);
        return ResultUtil.success();
    }

    // endregion 用户信息接口

    // region 用户配置接口

    /**
     * 获取用户基本配置
     */
    @GetMapping("/config/get")
    public BaseResponse<UserConfig> getUserConfig() {
        Long userId = securityUtils.getCurrentUserId();
        // 校验用户是否存在
        UserInfo userInfo = userService.findById(userId);
        if (userInfo == null) {
            return ResultUtil.error("user not found");
        }

        return ResultUtil.success(userService.getUserConfig(userId));
    }

    /**
     * 更新用户基本配置
     */
    @PostMapping("/config/update")
    public BaseResponse<?> updateUserConfig(@RequestBody BasicConfigUpdateRequest updateRequest) {
        Long userId = securityUtils.getCurrentUserId();
        userService.updateUserConfig(userId, updateRequest);
        return ResultUtil.success("update success");
    }

    /**
     * 获取用户模型服务商配置
     */
    @GetMapping("/config/model/get")
    public BaseResponse<List<UserLlmConfig>> getUserModelConfig() {
        Long userId = securityUtils.getCurrentUserId();
        // 校验用户是否存在
        UserInfo userInfo = userService.findById(userId);
        if (userInfo == null) {
            return ResultUtil.error("user not found");
        }

        return ResultUtil.success(userService.getUserLlmProviderConfig(userId));
    }

    /**
     * 更新用户模型服务商配置
     */
    @PostMapping("/config/model/update")
    public BaseResponse<?> updateUserModelConfig(@RequestBody List<UserLlmConfig> modeConfigList) {
        Long userId = securityUtils.getCurrentUserId();
        userService.updateModelConfig(userId, modeConfigList);
        return ResultUtil.success("update success");
    }

    @PostMapping("/config/model/active")
    public BaseResponse<?> activeSelectedModel(@RequestBody SelectedModel selectedModel) {
        Long userId = securityUtils.getCurrentUserId();
        selectedModel.setUserId(userId);
        userService.activeSelectedModel(selectedModel);
        return ResultUtil.success();
    }

    @PostMapping("/config/model/deactive")
    public BaseResponse<?> deactiveSelectedModel(@RequestBody SelectedModel selectedModel) {
        Long userId = securityUtils.getCurrentUserId();
        selectedModel.setUserId(userId);
        userService.deactiveSelectedModel(selectedModel);
        return ResultUtil.success();
    }

    @PostMapping("/config/model/list")
    public BaseResponse<?> getSelectedModel(@RequestParam("providerId") Integer providerId) {
        Long userId = securityUtils.getCurrentUserId();
        List<SelectedModel> selectedModelList = userService.getSelectedModel(userId, providerId);
        return ResultUtil.success(selectedModelList);
    }

    /**
     * 获取用户默认模型配置
     */
    @GetMapping("/config/default-model/get")
    public BaseResponse<List<UserDefaultModelConfig>> getUserDefaultModels() {
        Long userId = securityUtils.getCurrentUserId();
        // 校验用户是否存在
        UserInfo userInfo = userService.findById(userId);
        if (userInfo == null) {
            return ResultUtil.error("user not found");
        }

        return ResultUtil.success(userService.getUserDefaultModels(userId));
    }

    /**
     * 更新用户默认模型配置
     */
    @PostMapping("/config/default-model/update")
    public BaseResponse<?> updateUserDefaultModels(@RequestBody List<UserDefaultModelConfig> modelConfigs) {
        Long userId = securityUtils.getCurrentUserId();
        // 校验用户是否存在
        UserInfo userInfo = userService.findById(userId);
        if (userInfo == null) {
            return ResultUtil.error("user not found");
        }

        userService.updateUserDefaultModels(userId, modelConfigs);
        return ResultUtil.success("update success");
    }

    // endregion 用户配置接口

    // region 个人中心统计接口

    /**
     * 获取用户统计数据
     * @return 统计数据
     */
    @GetMapping("/stats")
    public BaseResponse<UserStatsVo> getUserStats() {
        Long userId = securityUtils.getCurrentUserId();
        // 校验用户是否存在
        UserInfo userInfo = userService.findById(userId);
        if (userInfo == null) {
            return ResultUtil.error("user not found");
        }

        return ResultUtil.success(userService.getUserStats(userId));
    }

    // endregion 个人中心统计接口

}
