/**
 * 知识图谱相关 API
 * 包含知识图谱数据获取等功能
 */
import { request } from './base'

export const knowledgeGraphApi = {
  // ========== 知识图谱 ==========

  /**
   * 获取简化版知识图谱数据
   * @returns {Promise<{nodes: Array, edges: Array}>} 知识图谱数据
   */
  getSimpleGraph() {
    return request('/knowledge-graph/simple', {
      method: 'GET'
    })
      .then((resp) => resp?.data || resp)
      .catch(() => ({ nodes: [], edges: [] }))
  }
}

export default knowledgeGraphApi
