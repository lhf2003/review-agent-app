/**
 * 测验和错题相关 API
 * 包含测验答题、错题本、知识点掌握度等功能
 */
import { request } from './base'

export const quizApi = {
  // ========== 答题相关 ==========

  /**
   * 提交单个答案
   */
  submitAnswer(questionId, userAnswer) {
    return request('/quiz/submit-answer', { method: 'POST', body: { questionId, userAnswer } })
  },

  /**
   * 批量提交答案
   */
  submitBatchAnswers(quizId, answers) {
    return request('/quiz/submit-batch-answers', { method: 'POST', body: { quizId, answers } })
  },

  /**
   * 重置测验
   */
  resetQuiz(quizId) {
    return request('/quiz/reset', { method: 'POST', body: { quizId } })
  },

  // ========== 测验历史 ==========

  /**
   * 获取测验历史
   * @param {Object} params - 查询参数
   * @param {number} params.status - 状态筛选（null=全部，0=进行中，1=已完成）
   * @param {number} params.collectionId - 合集筛选（null=全部合集）
   * @param {number} params.page - 页码（从0开始）
   * @param {number} params.size - 每页大小
   * @returns {Promise} 分页的习题历史
   */
  getQuizHistory(params) {
    return request('/quiz/history', { params })
  },

  /**
   * 获取测验详情
   * @param {number} quizId - 测验ID
   * @returns {Promise} 测验详情
   */
  getQuizDetail(quizId) {
    return request(`/quiz/${quizId}/detail`)
  },

  /**
   * 检测题库版本
   * @param {number} collectionId - 合集ID
   * @returns {Promise} 版本检测结果
   */
  checkQuizVersion(collectionId) {
    return request(`/quiz/collection/${collectionId}/version-check`)
  },

  /**
   * 重新生成题库
   * @param {number} collectionId - 合集ID
   * @returns {Promise} 新生成的QuizRecord
   */
  regenerateQuiz(collectionId) {
    return request(`/quiz/collection/${collectionId}/regenerate`, { method: 'POST' })
  },

  /**
   * 获取测验统计数据
   * @returns {Promise} 测验统计数据（分数趋势和知识点掌握度）
   */
  getQuizStats() {
    return request('/quiz/stats')
  },

  /**
   * 获取知识点掌握度列表
   */
  getKnowledgeMastery(limit = 20) {
    return request('/quiz/knowledge-mastery', { params: { limit } })
      .then(data => Array.isArray(data) ? data : [])
      .catch(() => [])
  },

  // ========== 错题本 ==========

  /**
   * 获取错题列表
   */
  getMistakeList(filter = 'all') {
    return request('/mistake-book/list', { params: { filter } })
      .then(data => Array.isArray(data) ? data : [])
  },

  /**
   * 获取错题统计
   */
  getMistakeStats() {
    return request('/mistake-book/stats')
  },

  /**
   * 获取复习推荐列表（基于遗忘曲线算法）
   */
  getReviewRecommendation() {
    return request('/mistake-book/review-recommendation')
      .then(data => Array.isArray(data) ? data : [])
      .catch(() => [])
  },

  /**
   * 标记错题为已掌握
   */
  markMistakesMastered(questionIds) {
    return request('/mistake-book/mark-mastered', { method: 'POST', body: { questionIds } })
  },

  /**
   * 删除错题
   */
  deleteMistakes(questionIds) {
    return request('/mistake-book/delete', { method: 'DELETE', body: { questionIds } })
  },

  // ========== 学习仪表盘 ==========

  /**
   * 获取学习仪表盘数据
   * @returns {Promise} 仪表盘数据（分数趋势、知识点雷达、热力图、时长分布、周统计）
   */
  getDashboard() {
    return request('/quiz/dashboard')
  },

  /**
   * 获取分数趋势数据（按时间范围）
   * @param {string} range - 时间范围（7/30/90/all）
   * @returns {Promise} 分数趋势数据
   */
  getDashboardTrend(range) {
    return request('/quiz/dashboard/trend', { params: { range } })
  }
}

export default quizApi
