package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.vo.SessionTraceVo;
import com.review.agent.service.SessionTraceService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 会话跟踪接口
 * 1. 获取文件的会话信息
 * 2. 将某些会话加入同一合集
 */
@RestController
@RequestMapping("/session-trace")
public class SessionTraceController {
    @Resource
    private SessionTraceService sessionTraceService;
    @Resource
    private SecurityUtils securityUtils;

    /**
     * 获取文件的会话信息
     * @param fileId 文件ID
     * @return 会话信息
     */
    @GetMapping("/get")
    public BaseResponse<SessionTraceVo> get(@RequestParam("fileId") Long fileId) {
        Long userId = securityUtils.getCurrentUserId();
        SessionTraceVo sessionTraceVo = sessionTraceService.getInfo(userId, fileId);
        return ResultUtil.success(sessionTraceVo);
    }
}