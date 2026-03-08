/**
 * 标签相关 API（重构版）
 * 支持多维度标签体系：技术领域、思维范式、难度等级、应用场景
 */
import { request } from './base'

export const tagApi = {
  // ========== 维度（新接口）==========

  /**
   * 获取标签维度列表
   * @returns {Promise<Array>} 维度列表 [{id, name, code, description, icon, sortOrder}]
   */
  getDimensionList() {
    return request('/tag/list')
  },

  // ========== 标签树（新接口）==========

  /**
   * 获取标签树结构
   * @param {number} dimensionId - 维度ID（可选）
   * @returns {Promise<Array>} 标签树列表
   */
  getTagTree(dimensionId) {
    const params = dimensionId != null ? { dimensionId } : undefined
    return request('/tag/tree', { params })
  },

  // ========== 技术领域标签（原主标签）==========

  /**
   * 获取技术领域标签列表（一级标签）
   * @returns {Promise<Array>} 标签列表
   */
  getMainTagList() {
    return request('/tag/tech-domain/list')
  },

  /**
   * 添加标签
   * @param {Object} tag - 标签对象 {name, dimensionId, parentId, level}
   * @returns {Promise<number>} 标签ID
   */
  addTag(tag) {
    return request('/tag/add', { method: 'POST', body: tag })
  },

  /**
   * 更新标签
   * @param {Object} tag - 标签对象 {id, name, ...}
   * @returns {Promise<void>}
   */
  updateTag(tag) {
    return request('/tag/update', { method: 'POST', body: tag })
  },

  /**
   * 删除标签
   * @param {number} id - 标签ID
   * @returns {Promise<void>}
   */
  deleteTag(id) {
    return request('/tag/delete', { method: 'DELETE', params: { id } })
  },

  // ========== 子标签（兼容旧接口）==========

  /**
   * 获取子标签列表
   * @param {number} parentId - 父标签ID
   * @returns {Promise<Array>} 子标签列表
   */
  getSubTagList(parentId) {
    return request('/tag/sub/list', { params: { parentId } })
  },

  /**
   * 添加子标签
   * @param {Object} tag - 标签对象 {name, parentId}
   * @returns {Promise<number>} 标签ID
   */
  addSubTag(tag) {
    return request('/tag/add/sub', { method: 'POST', body: tag })
  },

  // ========== 兼容旧 API（已废弃，保留以兼容旧代码）==========

  /**
   * @deprecated 使用 addTag 替代
   */
  addMainTag(tag) {
    return this.addTag({ ...tag, dimensionId: 1, level: 1 })
  },

  /**
   * @deprecated 使用 updateTag 替代
   */
  updateMainTag(tag) {
    return this.updateTag(tag)
  },

  /**
   * @deprecated 使用 deleteTag 替代
   */
  deleteMainTag(id) {
    return this.deleteTag(id)
  },

  /**
   * @deprecated 使用 updateTag 替代
   */
  updateSubTag(tag) {
    return this.updateTag(tag)
  },

  /**
   * @deprecated 使用 deleteTag 替代
   */
  deleteSubTag(id) {
    return this.deleteTag(id)
  },

  /**
   * @deprecated 新体系中标签关系通过 parentId 维护
   */
  getTagRelations() {
    // 返回空数组，新体系通过 parentId 维护关系
    return Promise.resolve([])
  },

  /**
   * @deprecated 新体系中标签关系通过 parentId 维护
   */
  addTagRelation() {
    // 新体系通过 parentId 维护关系
    return Promise.resolve()
  },

  /**
   * @deprecated 新体系中标签关系通过 parentId 维护
   */
  deleteTagRelation() {
    // 新体系通过 parentId 维护关系
    return Promise.resolve()
  }
}

export default tagApi
