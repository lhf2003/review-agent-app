/**
 * 通知相关 API
 * 包含通知设置、待复习提醒等功能
 */
import { request } from './base'

export const notificationApi = {
  /**
   * 获取通知设置
   */
  getNotificationSettings() {
    return request('/notification/settings')
  },

  /**
   * 更新通知设置
   */
  updateNotificationSettings(settings) {
    return request('/notification/settings', { method: 'PUT', body: settings })
  },

  /**
   * 获取待复习列表
   */
  getPendingReviews() {
    return request('/notification/pending')
      .then(data => Array.isArray(data) ? data : [])
      .catch(() => [])
  },

  /**
   * 获取待复习统计
   */
  getPendingReviewStats() {
    return request('/notification/pending/stats')
      .catch(() => ({
        totalPending: 0,
        mistakeCount: 0,
        quizCount: 0,
        highPriorityCount: 0
      }))
  }
}

export default notificationApi
