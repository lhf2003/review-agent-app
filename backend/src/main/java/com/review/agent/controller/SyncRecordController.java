package com.review.agent.controller;

import com.review.agent.service.SyncRecordService;
import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.entity.pojo.SyncRecord;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;


/**
 * 数据同步记录接口
 */
@RestController
@RequestMapping("/sync-record")
public class SyncRecordController {
    @Resource
    private SyncRecordService syncRecordService;

    @GetMapping("/history")
    public BaseResponse<java.util.List<SyncRecord>> history(@RequestHeader(value = "userId", required = false) Long userId) {
        java.util.List<SyncRecord> list = userId == null ? syncRecordService.findAll() : syncRecordService.findByUserId(userId);
        return ResultUtil.success(list);
    }
}
