/**
 * 标签相关 API
 * 包含主标签、子标签、标签关系等管理功能
 */
import { request } from './base'

export const tagApi = {
  // ========== 主标签 ==========

  /**
   * 获取主标签列表
   */
  getMainTagList() {
    return request('/tag/list')
  },

  /**
   * 添加主标签
   */
  addMainTag(mainTag) {
    return request('/tag/add', { method: 'POST', body: mainTag })
  },

  /**
   * 更新主标签
   */
  updateMainTag(mainTag) {
    return request('/tag/update', { method: 'POST', body: mainTag })
  },

  /**
   * 删除主标签
   */
  deleteMainTag(id) {
    return request('/tag/delete', { method: 'DELETE', params: { id } })
  },

  // ========== 子标签 ==========

  /**
   * 获取子标签列表
   */
  getSubTagList() {
    return request('/tag/sub/list')
  },

  /**
   * 添加子标签
   */
  addSubTag(subTag) {
    return request('/tag/add/sub', { method: 'POST', body: subTag })
  },

  /**
   * 更新子标签
   */
  updateSubTag(subTag) {
    return request('/tag/update/sub', { method: 'POST', body: subTag })
  },

  /**
   * 删除子标签
   */
  deleteSubTag(id) {
    return request('/tag/delete/sub', { method: 'DELETE', params: { id } })
  },

  // ========== 标签关系 ==========

  /**
   * 获取标签关系列表
   */
  getTagRelations(mainTagId) {
    const params = mainTagId != null ? { mainTagId } : undefined
    return request('/tag/list/relation', { params })
  },

  /**
   * 添加标签关系
   */
  addTagRelation(params) {
    return request('/tag/add/relation', { method: 'POST', body: params })
  },

  /**
   * 删除标签关系
   */
  deleteTagRelation(params) {
    return request('/tag/delete/relation', { method: 'DELETE', body: params })
  },

  // ========== 推荐标签 ==========

  /**
   * 添加推荐标签
   */
  addRecommendTag(body) {
    return request('/tag/recommand/add', { method: 'POST', body })
  }
}

export default tagApi
