package com.review.agent.controller;

import com.review.agent.common.constant.UserConstant;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.AesUtil;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.UserConfig;
import com.review.agent.entity.UserInfo;
import com.review.agent.entity.request.updatePasswordRequest;
import com.review.agent.entity.vo.UserInfoFilterVo;
import com.review.agent.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户信息接口
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

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
    public BaseResponse<UserInfoFilterVo> login(@RequestBody UserInfo userInfo) {
        // 解密前端传来的密码
        String plainPassword = AesUtil.decrypt(userInfo.getPassword());
        
        // 校验用户名密码是否正确
        UserInfo userInfoFromDb = userService.findByUsername(userInfo.getUsername());
        if (userInfoFromDb == null) {
            return ResultUtil.error("username not found");
        }
        if (!userInfoFromDb.getPassword().equals(DigestUtils.md5DigestAsHex((UserConstant.SALT + plainPassword).getBytes()))) {
            return ResultUtil.error("password not match");
        }
        UserInfoFilterVo userInfoFilterVo = new UserInfoFilterVo();
        BeanUtils.copyProperties(userInfoFromDb, userInfoFilterVo);
        return ResultUtil.success(userInfoFilterVo);
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
    public BaseResponse<UserInfoFilterVo> getUserInfoByUsername(@RequestHeader("userId") Long userId) {
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
    public BaseResponse<?> uploadAvatar(@RequestHeader("userId") Long userId, @RequestParam("avatar") MultipartFile avatar) {
        userService.uploadAvatar(userId, avatar);
        return ResultUtil.success("upload success");
    }

    /**
     * 更新用户信息
     */
    @PostMapping("/info/update")
    public BaseResponse<?> updateUserInfo(@RequestHeader("userId") Long userId, @RequestBody UserInfo userInfo) {
        userService.updateInfo(userId,userInfo);
        return ResultUtil.success();
    }

    /**
     * 更新用户密码
     */
    @PostMapping("/info/update/password")
    public BaseResponse<?> updateUserPassword(@RequestHeader("userId") Long userId, @RequestBody updatePasswordRequest request) {
        request.setOldPassword(AesUtil.decrypt(request.getOldPassword()));
        request.setNewPassword(AesUtil.decrypt(request.getNewPassword()));
        userService.updatePassword(userId,request);
        return ResultUtil.success("update success");
    }

    // endregion 用户信息接口

    // region 用户配置接口

    /**
     * 获取用户配置
     */
    @GetMapping("/config/get")
    public BaseResponse<UserConfig> getUserConfig(@RequestHeader("userId") Long userId) {
        // 校验用户是否存在
        UserInfo userInfo = userService.findById(userId);
        if (userInfo == null) {
            return ResultUtil.error("user not found");
        }

        return ResultUtil.success(userService.getUserConfig(userId));
    }

    /**
     * 更新用户配置
     */
    @PostMapping("/config/update")
    public BaseResponse<?> updateUserConfig(@RequestBody UserConfig userConfig, @RequestHeader(value = "userId", required = false) Long userId) {
        if (userId != null) {
            userConfig.setUserId(userId);
        }
        userService.updateUserConfig(userConfig);
        return ResultUtil.success("update success");
    }
    // endregion 用户配置接口

}