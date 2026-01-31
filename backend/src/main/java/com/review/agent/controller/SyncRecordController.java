package com.review.agent.controller;

import com.review.agent.common.utils.SecurityUtils;
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
    @Resource
    private SecurityUtils securityUtils;

    @GetMapping("/history")
    public BaseResponse<java.util.List<SyncRecord>> history() {
        Long userId = securityUtils.getCurrentUserId();
        java.util.List<SyncRecord> list = syncRecordService.findByUserId(userId);
        return ResultUtil.success(list);
    }
}
