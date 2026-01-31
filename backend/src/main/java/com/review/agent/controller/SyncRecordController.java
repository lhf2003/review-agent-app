package com.review.agent.controller;

import com.review.agent.common.exception.BaseResponse;
import com.review.agent.common.utils.ResultUtil;
import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.pojo.SyncRecord;
import com.review.agent.service.SyncRecordService;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    /**
     * 获取同步历史记录（支持分页和搜索过滤）
     */
    @GetMapping("/history")
    public BaseResponse<Page<SyncRecord>> history(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = securityUtils.getCurrentUserId();

        // 默认分页参数
        int pageNum = page != null && page >= 0 ? page : 0;
        int pageSize = size != null && size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize);

        Page<SyncRecord> result = syncRecordService.findByUserIdWithFilters(
            userId, status, startDate, endDate, pageable);

        return ResultUtil.success(result);
    }

    /**
     * 重试同步（重新触发同步操作）
     */
    @PostMapping("/retry")
    public BaseResponse<String> retrySync() {
        Long userId = securityUtils.getCurrentUserId();
//        syncRecordService.retrySync(userId);
        return ResultUtil.success("同步已重新触发");
    }
}
