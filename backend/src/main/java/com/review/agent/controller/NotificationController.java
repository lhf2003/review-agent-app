package com.review.agent.controller;

import com.review.agent.common.utils.SecurityUtils;
import com.review.agent.entity.vo.NotificationSettingsVO;
import com.review.agent.entity.vo.PendingReviewVO;
import com.review.agent.service.ReviewReminderService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知设置控制器
 * 提供复习提醒相关的API接口
 */
@Slf4j
@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Resource
    private ReviewReminderService reviewReminderService;

    @Resource
    private SecurityUtils securityUtils;

    /**
     * 获取通知设置
     *
     * @return 通知设置
     */
    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingsVO> getSettings() {
        Long userId = securityUtils.getCurrentUserId();
        NotificationSettingsVO settings = reviewReminderService.getNotificationSettings(userId);
        return ResponseEntity.ok(settings);
    }

    /**
     * 更新通知设置
     *
     * @param vo 通知设置VO
     * @return 更新后的设置
     */
    @PutMapping("/settings")
    public ResponseEntity<NotificationSettingsVO> updateSettings(@RequestBody NotificationSettingsVO vo) {
        Long userId = securityUtils.getCurrentUserId();
        NotificationSettingsVO updated = reviewReminderService.updateNotificationSettings(userId, vo);
        return ResponseEntity.ok(updated);
    }

    /**
     * 获取待复习列表
     *
     * @return 待复习项列表
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PendingReviewVO>> getPendingReviews() {
        Long userId = securityUtils.getCurrentUserId();
        List<PendingReviewVO> pendingReviews = reviewReminderService.getPendingReviews(userId);
        return ResponseEntity.ok(pendingReviews);
    }

    /**
     * 获取待复习统计
     *
     * @return 统计信息
     */
    @GetMapping("/pending/stats")
    public ResponseEntity<Map<String, Object>> getPendingReviewStats() {
        Long userId = securityUtils.getCurrentUserId();
        ReviewReminderService.PendingReviewStats stats = reviewReminderService.getPendingReviewStats(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("totalPending", stats.totalPending());
        result.put("mistakeCount", stats.mistakeCount());
        result.put("quizCount", stats.quizCount());
        result.put("highPriorityCount", stats.highPriorityCount());

        return ResponseEntity.ok(result);
    }
}
