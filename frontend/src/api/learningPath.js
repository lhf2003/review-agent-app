/**
 * 学习路径 API
 * 基于标签关系的个性化学习路径推荐
 */
import { request } from './base'

export const learningPathApi = {
  /**
   * 获取学习路径推荐
   * @param {number} limit - 限制数量（默认5）
   * @returns {Promise<Array>} 学习路径列表
   */
  getRecommendations(limit = 5) {
    return request('/learning-path/recommendations', { params: { limit } })
  },

  /**
   * 获取特定标签的学习路径
   * @param {number} tagId - 标签ID
   * @returns {Promise<Object>} 学习路径详情
   */
  getLearningPathForTag(tagId) {
    return request(`/learning-path/tag/${tagId}`)
  },

  /**
   * 获取学习进度概览
   * @returns {Promise<Object>} 学习进度统计
   */
  getLearningProgressOverview() {
    return request('/learning-path/progress-overview')
  }
}

export default learningPathApi
