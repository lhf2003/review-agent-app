/**
 * 分析相关 API
 * 包含分析列表、分析结果、会话追踪、相似问题等功能
 */
import { request, handleStream, getToken, getUserId } from './base'

export const analysisApi = {
  // ========== 分析列表 ==========

  /**
   * 获取分析结果分页列表
   */
  getAnalysisList(params) {
    const page = params?.page ?? 0
    const size = params?.size ?? 10

    const body = {
      userId: getUserId(),
      ...(params || {})
    }
    delete body.page
    delete body.size

    return request('/analysis/page', {
      method: 'POST',
      params: { page, size },
      body
    })
      .then((resp) => {
        const list = resp?.data || resp
        if (Array.isArray(list)) return { list, total: list.length }
        return resp
      })
      .catch(() => ({
        list: [
          { id: 1, title: 'useEffect无限循环', problemStatement: 'useEffect依赖数组缺失导致重复执行', rootCause: '依赖数组未声明', mastery: 40, tags: ['React', 'useEffect'], date: '2025-09-12' },
          { id: 2, title: 'Python异常处理不当', problemStatement: '未捕获特定异常导致程序崩溃', rootCause: '错误处理泛化', mastery: 60, tags: ['Python', '错误处理'], date: '2025-10-01' },
          { id: 3, title: '状态管理冗余更新', problemStatement: '多次 setState 导致性能问题', rootCause: '缺少批处理', mastery: 35, tags: ['React'], date: '2025-08-19' },
        ],
        total: 3,
      }))
  },

  /**
   * 获取指定文件的分析列表
   * ⚠️ 后端暂未提供此接口，使用 mock 数据
   */
  getAnalysisByFile(fileId) {
    // return request('/analysis/by-file', { params: { fileId } })
    console.warn('[API] getAnalysisByFile 使用 mock 数据，后端接口暂未实现')
    return Promise.resolve({
      list: [
        { id: 11, title: '示例详情', problemStatement: '示例问题详情', rootCause: '示例根因', mastery: 50, tags: ['Demo'], date: '2025-11-29' },
      ],
    })
  },

  /**
   * 启动分析
   */
  startAnalysis(fileId) {
    return request('/analysis/start', { method: 'GET', params: { fileId } })
  },

  /**
   * 获取分析结果（仅 dataId）
   */
  getAnalysisResult(dataId) {
    return request('/analysis/result', { params: { dataId } })
  },

  /**
   * 获取分析结果（同时传 dataId 与 analysisId）
   */
  getAnalysisResultByIds(dataId, analysisId) {
    return request('/analysis/result', { params: { dataId, analysisId } })
  },

  // ========== 文件名列表 ==========

  /**
   * 获取文件名列表
   */
  getFileNameList() {
    return request('/analysis/file-name/list')
  },

  // ========== 标签统计 ==========

  /**
   * 获取标签统计
   */
  getTagStats() {
    return request('/analysis/tag/list')
  },

  // ========== 相似问题 ==========

  /**
   * 获取相似问题
   */
  getSimilarIssues(analysisResultId) {
    return request('/analysis/similarity', { params: { analysisId: analysisResultId } })
      .then((resp) => resp?.data || resp)
  },

  // ========== 会话追踪 ==========

  /**
   * 获取会话追踪
   */
  getSessionTrace(fileId) {
    return request('/session-trace/get', { params: { fileId } })
  },

  // ========== SSE 流式分析 ==========

  /**
   * 分析日志流
   */
  analysisLogStream(handlers = {}) {
    const controller = new AbortController()
    const token = getToken()
    const headers = { Accept: 'text/event-stream' }
    if (token) headers['Authorization'] = `Bearer ${token}`

    const p = fetch('/api/analysis/log/stream', { method: 'GET', headers, signal: controller.signal })
    handleStream(p, handlers)
    return { cancel: () => controller.abort() }
  }
}

export default analysisApi
