/**
 * 数据和文件相关 API
 * 包含文件导入、数据页管理等功能
 */
import { request, getToken, BASE_URL, getUserId } from './base'

export const dataApi = {
  // ========== 文件操作 ==========

  /**
   * 导入文件
   */
  importFile(file) {
    const formData = new FormData()
    formData.append('file', file)

    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`

    return fetch(BASE_URL + '/file-info/import', {
      method: 'POST',
      headers,
      body: formData,
    }).then(async (res) => {
      if (!res.ok) throw new Error(await res.text())
      const json = await res.json()
      return normalizeResponse(json)
    })
  },

  /**
   * 更新文件状态
   * ⚠️ 后端暂未提供此接口，暂时禁用
   */
  updateFileStatus(id, status) {
    // return request('/file-info/status', { method: 'PATCH', params: { id, status } })
    console.warn('[API] updateFileStatus 接口后端暂未实现')
    return Promise.reject(new Error('接口未实现'))
  },

  // ========== 数据页操作 ==========

  /**
   * 获取数据分页列表
   */
  dataPage(params) {
    const page = params?.page ?? 0
    const size = params?.size ?? 10
    const body = {
      userId: getUserId(),
      fileName: params?.fileName ?? null,
      processedStatus: params?.processedStatus ?? null,
      source: params?.source ?? null
    }
    return request('/data/page', { method: 'POST', params: { page, size }, body })
  },

  /**
   * 创建数据记录
   */
  dataCreate(body) {
    return request('/data/create', { method: 'POST', body })
  },

  /**
   * 导入数据文件
   */
  dataImport(userId, file, source) {
    const formData = new FormData()
    formData.append('file', file)
    if (source !== undefined && source !== null) formData.append('source', source)

    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`

    return fetch(BASE_URL + '/data/import', { method: 'POST', headers, body: formData }).then(async (res) => {
      if (!res.ok) throw new Error(await res.text())
      const json = await res.json()
      return normalizeResponse(json)
    })
  },

  /**
   * 更新数据状态
   * ⚠️ 后端暂未提供此接口，暂时禁用
   */
  dataUpdateStatus(id, status) {
    // return request('/data/status', { method: 'PATCH', params: { id, status } })
    console.warn('[API] dataUpdateStatus 接口后端暂未实现')
    return Promise.reject(new Error('接口未实现'))
  },

  /**
   * 删除数据
   */
  dataDelete(id) {
    return request('/data/delete', { method: 'DELETE', params: { id } })
  },

  // ========== 同步历史 ==========

  /**
   * 获取同步历史
   */
  getSyncHistory() {
    return request('/sync-record/history')
  }
}

/**
 * 响应标准化函数（本地使用）
 */
function normalizeResponse(resp) {
  if (resp == null) return resp
  const hasCode = Object.prototype.hasOwnProperty.call(resp, 'code')
  if (!hasCode) return resp
  const codeNum = Number(resp.code)
  if (Number.isNaN(codeNum)) return resp
  if (codeNum === 0) {
    return resp.data !== undefined ? resp.data : resp
  }
  const msg = resp.message || resp.msg || '请求失败'
  throw new Error(msg)
}

export default dataApi
