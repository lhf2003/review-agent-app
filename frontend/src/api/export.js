/**
 * 导出相关 API
 * 包含分析结果导出、合集导出等功能
 */
import { getToken } from './base'

export const exportApi = {
  /**
   * 导出单个分析结果为 Markdown
   */
  exportAnalysis(id) {
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`

    return fetch('/api/export/analysis/' + id, {
      method: 'GET',
      headers
    }).then(async (res) => {
      if (!res.ok) {
        const text = await res.text()
        throw new Error(text || `HTTP ${res.status}`)
      }
      return res.blob()
    })
  },

  /**
   * 批量导出分析结果为 Markdown
   */
  exportBatchAnalysis(ids) {
    const token = getToken()
    const headers = {
      'Content-Type': 'application/json'
    }
    if (token) headers['Authorization'] = `Bearer ${token}`

    return fetch('/api/export/analysis/batch', {
      method: 'POST',
      headers,
      body: JSON.stringify(ids)
    }).then(async (res) => {
      if (!res.ok) {
        const text = await res.text()
        throw new Error(text || `HTTP ${res.status}`)
      }
      return res.blob()
    })
  },

  /**
   * 导出合集为 Markdown
   */
  exportCollection(id) {
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`

    return fetch('/api/export/collection/' + id, {
      method: 'GET',
      headers
    }).then(async (res) => {
      if (!res.ok) {
        const text = await res.text()
        throw new Error(text || `HTTP ${res.status}`)
      }
      return res.blob()
    })
  },

  /**
   * 下载 Blob 文件
   */
  downloadBlob(blob, filename) {
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  }
}

export default exportApi
