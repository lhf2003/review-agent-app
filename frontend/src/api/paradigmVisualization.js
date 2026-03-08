/**
 * 思维范式可视化 API
 * 支持分析结果的思维范式流程图展示
 */
import { request } from './base'

export const paradigmVisualizationApi = {
  /**
   * 获取分析结果的思维范式可视化列表
   * @param {number} analysisResultId - 分析结果ID
   * @returns {Promise<Array>} 可视化列表 [{id, analysisResultId, paradigmCode, paradigmName, paradigmType, flowchartData}]
   */
  getVisualizationsByResult(analysisResultId) {
    return request(`/paradigm-visualization/result/${analysisResultId}`)
  },

  /**
   * 获取流程图详情
   * @param {number} flowchartId - 流程图ID
   * @returns {Promise<Object>} 流程图详情 {id, nodes, edges, layout}
   */
  getFlowchartDetail(flowchartId) {
    return request(`/paradigm-visualization/flowchart/${flowchartId}`)
  },

  /**
   * 获取知识图谱范式层数据
   * @param {string[]} paradigmCodes - 范式代码列表
   * @returns {Promise<Object>} 图谱数据 {nodes, edges}
   */
  getGraphLayer(paradigmCodes) {
    return request('/paradigm-visualization/graph-layer', {
      params: { paradigmCodes }
    })
  },

  /**
   * 重新生成可视化
   * @param {number} analysisResultId - 分析结果ID
   * @returns {Promise<void>}
   */
  regenerateVisualization(analysisResultId) {
    return request(`/paradigm-visualization/regenerate/${analysisResultId}`, {
      method: 'POST'
    })
  }
}

export default paradigmVisualizationApi
