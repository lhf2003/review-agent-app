package com.review.agent.controller;

import com.review.agent.common.constant.UserConstant;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.UserConfig;
import com.review.agent.entity.UserInfo;
import com.review.agent.entity.vo.UserInfoFilterVo;
import com.review.agent.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

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
        userService.register(userInfo);
        return ResultUtil.success("register success");
    }

    @PostMapping("/login")
    public BaseResponse<UserInfoFilterVo> login(@RequestBody UserInfo userInfo) {
        // 校验用户名密码是否正确
        UserInfo userInfoFromDb = userService.findByUsername(userInfo.getUsername());
        if (userInfoFromDb == null) {
            return ResultUtil.error("username not found");
        }
        if (!userInfoFromDb.getPassword().equals(DigestUtils.md5DigestAsHex((UserConstant.SALT + userInfo.getPassword()).getBytes()))) {
            return ResultUtil.error("password not match");
        }
        UserInfoFilterVo userInfoFilterVo = new UserInfoFilterVo();
        BeanUtils.copyProperties(userInfoFromDb, userInfoFilterVo);
        return ResultUtil.success(userInfoFilterVo);
    }

    @GetMapping("/logout")
    public BaseResponse<?> getUserInfo(@RequestParam("id") Long id) {
        UserInfo userInfo = userService.findById(id);
        if (userInfo == null) {
            return ResultUtil.error("user id not found");
        }
        return ResultUtil.success(userInfo);
    }
    @GetMapping("/by-username")
    public BaseResponse<UserInfoFilterVo> getUserInfoByUsername(@RequestParam("username") String username) {
        UserInfo userInfo = userService.findByUsername(username);
        if (userInfo == null) {
            return ResultUtil.error("username not found");
        }
        UserInfoFilterVo userInfoFilterVo = new UserInfoFilterVo();
        BeanUtils.copyProperties(userInfo, userInfoFilterVo);
        return ResultUtil.success(userInfoFilterVo);
    }

    @PostMapping("/info/update")
    public BaseResponse<?> updateUserInfo(@RequestBody UserInfo userInfo) {
        userService.updateInfo(userInfo);
        return ResultUtil.success("update success");
    }

    // endregion 用户信息接口

    // region 用户配置接口

    /**
     * 获取用户配置
     */
    @GetMapping("/config/get")
    public BaseResponse<UserConfig> getUserConfig(@RequestParam("userId") Long userId) {
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
    public BaseResponse<?> updateUserConfig(@RequestBody UserConfig userConfig) {
        userService.updateUserConfig(userConfig);
        return ResultUtil.success("update success");
    }
    // endregion 用户配置接口

}