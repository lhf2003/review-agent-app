/**
 * 统计报表相关 API
 * 包含词云、标签趋势、报表等功能
 */
import { request } from './base'

export const statisticApi = {
  // ========== 词云 ==========

  /**
   * 获取词云数据
   */
  getWordReport(startDate, endDate) {
    return request('/statistic/word-cloud', {
      method: 'POST',
      body: { startDate, endDate }
    })
      .then((resp) => resp?.data || resp)
      .catch(() => ({ 并发: 2, Java: 1, 性能优化: 2, 基础语法: 2, SQL: 1 }))
  },

  // ========== 标签趋势 ==========

  /**
   * 获取标签趋势数据
   */
  getDateTagCountTrend(startDate, endDate) {
    return request('/statistic/tag/trend', {
      method: 'POST',
      body: { startDate, endDate }
    })
  },

  // ========== 报表 ==========

  /**
   * 获取报表列表
   */
  getReportList(type = 1, date) {
    const params = { type }
    if (date) params.date = date
    return request('/report/get', { params })
  }
}

export default statisticApi
