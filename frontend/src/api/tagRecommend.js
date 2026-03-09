/**
 * 推荐标签管理 API
 * 处理AI分析生成的推荐标签的查询、采纳、忽略等操作
 */
import { request } from './base'

export const tagRecommendApi = {
  /**
   * 获取推荐标签列表
   * 返回按状态分组的结构：{ PENDING: [], ADOPTED: [], IGNORED: [] }
   * 其中IGNORED只返回最近5条
   */
  getRecommendations() {
    return request('/tag/recommendations')
  },

  /**
   * 获取推荐标签统计信息
   */
  getStats() {
    return request('/tag/recommendations/stats')
  },

  /**
   * 采纳推荐标签
   * @param {number} id - 推荐标签ID
   */
  adoptRecommendation(id) {
    return request(`/tag/recommendations/${id}/adopt`, { method: 'POST' })
  },

  /**
   * 忽略推荐标签
   * @param {number} id - 推荐标签ID
   */
  ignoreRecommendation(id) {
    return request(`/tag/recommendations/${id}/ignore`, { method: 'POST' })
  },

  /**
   * 批量采纳推荐标签
   * @param {number[]} ids - 推荐标签ID列表
   */
  adoptAll(ids) {
    return request('/tag/recommendations/adopt-all', {
      method: 'POST',
      body: ids
    })
  },

  /**
   * 批量忽略推荐标签
   * @param {number[]} ids - 推荐标签ID列表
   */
  ignoreAll(ids) {
    return request('/tag/recommendations/ignore-all', {
      method: 'POST',
      body: ids
    })
  }
}

export default tagRecommendApi
