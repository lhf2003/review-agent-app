package com.review.agent.controller;

import com.review.agent.common.UserConstant;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.exception.ErrorCode;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.UserInfo;
import com.review.agent.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/register")
    public BaseResponse<?> register(@RequestBody UserInfo userInfo) {
        userService.register(userInfo);
        return ResultUtil.success("register success");
    }

    @PostMapping("/login")
    public BaseResponse<?> login(@RequestBody UserInfo userInfo) {
        // 校验用户名密码是否正确
        UserInfo userInfoFromDb = userService.findByUsername(userInfo.getUsername());
        if (userInfoFromDb == null) {
            return ResultUtil.error("username not found");
        }
        if (!userInfoFromDb.getPassword().equals(DigestUtils.md5DigestAsHex((UserConstant.SALT + userInfo.getPassword()).getBytes()))) {
            return ResultUtil.error( "password not match");
        }
        return ResultUtil.success("login success");
    }

    @GetMapping("/logout")
    public BaseResponse<?> getUserInfo(@RequestParam("id") Long id) {
        UserInfo userInfo = userService.findById(id);
        if (userInfo == null) {
            return ResultUtil.error("user id not found");
        }
        return ResultUtil.success(userInfo);
    }

    @PostMapping("/info/update")
    public BaseResponse<?> updateUserInfo(@RequestBody UserInfo userInfo) {
        userService.updateInfo(userInfo);
        return ResultUtil.success("update success");
    }

}