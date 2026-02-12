/**
 * 合集相关 API
 * 包含合集管理、测验生成等功能
 */
import { request } from './base'

export const collectionApi = {
  // ========== 合集管理 ==========

  /**
   * 获取合集列表
   */
  getCollectionList() {
    return request('/collection/list')
      .then(data => ({ list: Array.isArray(data) ? data : [] }))
      .catch(() => ({
        list: [
          { id: 1, name: 'React 性能优化', description: '关于 React 渲染与 Hooks 的常见问题', count: 5, updatedAt: '2025-10-01' },
          { id: 2, name: '后端并发编程', description: 'Java 线程池与锁机制', count: 3, updatedAt: '2025-09-20' }
        ]
      }))
  },

  /**
   * 创建合集
   */
  createCollection(data) {
    return request('/collection/add', { method: 'POST', body: data })
      .then(res => (typeof res === 'number' || typeof res === 'string' ? { id: res } : res))
  },

  /**
   * 更新合集
   */
  updateCollection(id, data) {
    return request('/collection/update', { method: 'PUT', body: data, params: { id } })
  },

  /**
   * 删除合集
   */
  deleteCollection(id) {
    return request('/collection/delete', { method: 'DELETE', params: { id } })
  },

  /**
   * 获取合集详情
   */
  getCollectionDetail(id) {
    return request('/collection/detail', { params: { id } })
  },

  // ========== 合集条目管理 ==========

  /**
   * 添加会话到合集
   */
  addSessionToCollection(data) {
    const body = { action: 'ADD', analysisIds: [data.sessionId] }
    return request('/collection/items', { method: 'POST', body, params: { collectionId: data.collectionId } })
  },

  /**
   * 从合集中移除会话
   */
  removeSessionFromCollection(collectionId, sessionId) {
    const body = { action: 'REMOVE', analysisIds: [sessionId] }
    return request('/collection/items', { method: 'POST', body, params: { collectionId } })
  },

  // ========== 测验生成 ==========

  /**
   * 生成测验
   */
  generateQuiz(collectionId) {
    return request(`/quiz/collection/${collectionId}/generate`, { method: 'POST', body: { collectionId } })
  },

  // ========== 知识点推荐 ==========

  /**
   * 获取合集推荐列表（基于薄弱知识点）
   */
  getCollectionRecommendations(limit = 8) {
    return request('/collection/recommendations', { params: { limit } })
      .then(data => Array.isArray(data) ? data : [])
      .catch(() => [])
  },

  // ========== 智能推荐创建合集 ==========

  /**
   * 获取智能推荐合集
   * @returns {Promise} 推荐列表和未归档统计
   */
  getSmartRecommendations() {
    return request('/collection/smart-recommendations')
      .then(data => data || { recommendations: [], totalUnarchived: 0 })
      .catch(() => ({ recommendations: [], totalUnarchived: 0 }))
  },

  /**
   * 快速创建合集
   * @param {Object} data - 创建数据
   * @param {string} data.name - 合集名称
   * @param {string} data.description - 合集描述
   * @param {number[]} data.analysisIds - 分析结果ID列表
   * @returns {Promise} 创建结果
   */
  quickCreateCollection(data) {
    return request('/collection/quick-create', { method: 'POST', body: data })
  },

  /**
   * 忽略推荐
   * @param {string} recommendationId - 推荐ID
   * @returns {Promise}
   */
  dismissRecommendation(recommendationId) {
    return request(`/collection/recommendations/${recommendationId}/dismiss`, { method: 'POST' })
  }
}

export default collectionApi
