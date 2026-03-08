/**
 * 标签关系 API
 * 支持标签依赖、相似、互补等关系的查询和管理
 */
import { request } from './base'

export const tagRelationApi = {
  // ========== 标签关系查询（统一接口）==========

  /**
   * 获取标签关系（统一接口）
   * @param {number} tagId - 标签ID
   * @param {string} type - 关系类型：all(所有关系), dependencies(前置依赖), next(推荐下一步), similar(相似标签), complementary(互补标签)
   * @returns {Promise<Array>} 关系列表
   */
  getTagRelations(tagId, type = 'all') {
    return request(`/tag-relation/${tagId}`, { params: { type } })
  },

  /**
   * 获取标签的所有关系
   * @param {number} tagId - 标签ID
   * @returns {Promise<Array>} 关系列表 [{id, sourceTagId, sourceTagName, targetTagId, targetTagName, relationType, relationTypeLabel, strength, evidence, autoDetected, direction}]
   */
  getAllRelations(tagId) {
    return this.getTagRelations(tagId, 'all')
  },

  /**
   * 获取标签的前置依赖（需要先学什么）
   * @param {number} tagId - 标签ID
   * @returns {Promise<Array>} 依赖标签列表 [{id, name, strength}]
   */
  getTagDependencies(tagId) {
    return this.getTagRelations(tagId, 'dependencies')
  },

  /**
   * 获取推荐学习的下一个标签（基于依赖关系）
   * @param {number} tagId - 标签ID
   * @returns {Promise<Array>} 推荐标签列表 [{id, name, strength}]
   */
  getRecommendedNextTags(tagId) {
    return this.getTagRelations(tagId, 'next')
  },

  /**
   * 获取相似标签
   * @param {number} tagId - 标签ID
   * @returns {Promise<Array>} 相似标签列表 [{id, name, strength}]
   */
  getSimilarTags(tagId) {
    return this.getTagRelations(tagId, 'similar')
  },

  /**
   * 获取互补标签
   * @param {number} tagId - 标签ID
   * @returns {Promise<Array>} 互补标签列表 [{id, name, strength}]
   */
  getComplementaryTags(tagId) {
    return this.getTagRelations(tagId, 'complementary')
  },

  // ========== 标签关系管理 ==========

  /**
   * 创建标签关系
   * @param {Object} relation - 关系对象 {sourceTagId, targetTagId, relationType, strength, evidence}
   * @returns {Promise<Object>} 创建的关系
   */
  createRelation(relation) {
    return request('/tag-relation/create', { method: 'POST', body: relation })
  },

  /**
   * 删除标签关系
   * @param {number} relationId - 关系ID
   * @returns {Promise<void>}
   */
  deleteRelation(relationId) {
    return request(`/tag-relation/delete/${relationId}`, { method: 'DELETE' })
  },

  /**
   * 更新关系强度
   * @param {number} relationId - 关系ID
   * @param {number} strength - 强度值(1-100)
   * @returns {Promise<Object>} 更新后的关系
   */
  updateRelationStrength(relationId, strength) {
    return request(`/tag-relation/update-strength/${relationId}`, {
      method: 'POST',
      params: { strength }
    })
  },

  // ========== 标签关系发现 ==========

  /**
   * 触发单个分析结果的标签关系发现
   * @param {number} analysisResultId - 分析结果ID
   * @returns {Promise<void>}
   */
  discoverRelationsForAnalysis(analysisResultId) {
    return request(`/tag-relation/discover/${analysisResultId}`, { method: 'POST' })
  },

  /**
   * 批量发现用户的标签关系（基于共现）- 异步
   * @returns {Promise<{taskId: number, status: string, message: string}>} 任务信息
   */
  discoverRelationsByCoOccurrence() {
    return request('/tag-relation/discover-co-occurrence', { method: 'POST' })
  },

  /**
   * 查询关系发现任务状态
   * @param {number} taskId - 任务ID
   * @returns {Promise<TaskVO>} 任务状态
   */
  getDiscoveryTaskStatus(taskId) {
    return request(`/tag-relation/discover-task/${taskId}`)
  }
}

export default tagRelationApi
