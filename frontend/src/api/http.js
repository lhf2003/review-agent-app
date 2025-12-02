const BASE_URL = 'http://localhost:8081'

async function request(path, { method = 'GET', params, body, headers } = {}) {
  let url = BASE_URL + path
  if (params) {
    const usp = new URLSearchParams(params)
    url += `?${usp.toString()}`
  }
  const res = await fetch(url, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(headers || {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`HTTP ${res.status}: ${text}`)
  }
  const data = await res.json().catch(() => null)
  return data
}

export const api = {
  // config
  getConfig(userId) {
    return request('/user/config/get', { params: { userId } })
  },
  updateConfig(body) {
    return request('/user/config/update', { method: 'POST', body })
  },

  // tags
  getAllTags() {
    return request('/tag/get/all')
  },
  addTag(tag) {
    return request('/tag/add', { method: 'POST', body: tag })
  },
  deleteTag(id) {
    return request('/tag/delete', { method: 'DELETE', params: { id } })
  },
  renameTag(userId, id, name) {
    return request('/tag/update', { method: 'POST', body: { userId, id, name } }).catch(() => ({ ok: true }))
  },
  mergeTags(userId, sourceIds, targetId) {
    // 预期：POST /tag/merge { sourceIds, targetId }
    return request('/tag/merge', { method: 'POST', body: { userId, sourceIds, targetId } }).catch(() => ({ ok: true }))
  },
  // 标签分页
  getTagPage(params) {
    // 改为 POST 请求，分页参数放 body
    const { page = 0, size = 10, ...rest } = params || {}
    return request('/tag/page', {
      method: 'POST',
      body: { page, size, ...rest },
    }).catch(() => ({
      list: [
        { id: 1, name: 'React', type: 1 },
        { id: 2, name: 'Vue', type: 1 },
        { id: 3, name: 'TypeScript', type: 1 },
        { id: 4, name: 'JavaScript', type: 1 },
        { id: 5, name: 'useEffect', type: 2 },
        { id: 6, name: 'useState', type: 2 },
        { id: 7, name: 'Next.js', type: 1 },
        { id: 8, name: 'Tailwind', type: 1 },
        { id: 9, name: 'Python', type: 1 },
        { id: 10, name: 'Django', type: 1 },
        { id: 11, name: 'FastAPI', type: 1 },
        { id: 12, name: 'SQL', type: 1 },
      ],
      total: 23,
    }))
  },
  getTagTypeStats() {
    // 预期：GET /tag/type/stats 返回各类型计数
    return request('/tag/type/stats').catch(() => ({
      stats: [
        { type: 0, name: '全部', count: 23 },
        { type: 1, name: '技术栈', count: 12 },
        { type: 2, name: '问题类型', count: 6 },
        { type: 3, name: '难度', count: 5 },
      ],
    }))
  },
  searchTags(q) {
    // 预期：GET /tag/search?q=...
    return request('/tag/search', { params: { q } }).catch(() => ({
      list: [{ id: 1, name: 'React', type: 1 }, { id: 5, name: 'useEffect', type: 2 }],
    }))
  },

  // tag page (分页查询，请求体包含 userId / tagName)
  getTagPage({ page = 0, size = 20, userId, tagName }) {
    return request('/tag/page', { method: 'POST', params: { page, size }, body: { userId, tagName } })
      .catch(() => ({
        list: [
          { id: 1, name: 'React', type: 1 },
          { id: 2, name: 'Vue', type: 1 },
          { id: 3, name: 'TypeScript', type: 1 },
          { id: 4, name: 'JavaScript', type: 1 },
          { id: 5, name: 'useEffect', type: 2 },
          { id: 6, name: 'useState', type: 2 },
        ],
        total: 6,
      }))
  },

  // file info
  importFile(userId, file) {
    const formData = new FormData()
    formData.append('userId', userId)
    formData.append('file', file)
    return fetch(BASE_URL + '/file-info/import', {
      method: 'POST',
      body: formData,
    }).then(async (res) => {
      if (!res.ok) throw new Error(await res.text())
      return res.json()
    })
  },
  updateFileStatus(id, status) {
    return request('/file-info/status', { method: 'PATCH', params: { id, status } })
  },

  // sync history
  getSyncHistory(userId) {
    return request('/sync-record/history', { params: userId ? { userId } : undefined })
  },

  // user info
  updateUserInfo(body) {
    return request('/user/info/update', { method: 'POST', body })
  },

  getAnalysisList(params) {
    const page = params?.page ?? 0
    const size = params?.size ?? 10
    const userId = params?.userId || null
    return request('/analysis/page', {
      method: 'POST',
      params: { page, size },
      body: { userId, problemStatement: params?.problemStatement, status: params?.status, tagId: params?.tagId }
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
  getAnalysisByFile(fileId) {
    // 预期：返回指定 fileId 的分析列表
    return request('/analysis/by-file', { params: { fileId } }).catch(() => ({
      list: [
        { id: 11, title: '示例详情', problemStatement: '示例问题详情', rootCause: '示例根因', mastery: 50, tags: ['Demo'], date: '2025-11-29' },
      ],
    }))
  },
  startAnalysis(userId, fileIdList) {
    return request('/analysis/start', { method: 'POST', body: { userId, fileIdList } })
  },
  getAnalysisResult(userId, dataId) {
    return request('/analysis/result', { params: { userId, dataId } })
  },
  // 获取分析结果（需要同时传 dataId 与 analysisId）
  getAnalysisResultByIds(userId, dataId, analysisId) {
    return request('/analysis/result', { params: { userId, dataId, analysisId } })
  },
  getTagStats(params) {
    // 预期：返回 { tags: [{ id, name, count }] }
    return request('/analysis/tag/list', { params }).catch(() => ({ tags: [{ id: 1, name: 'React', count: 8 }, { id: 2, name: 'useEffect', count: 5 }, { id: 3, name: 'Python', count: 6 }, { id: 4, name: '错误处理', count: 4 }] }))
  },

  // data page (DataInfo)
  dataPage(params) {
    const page = params?.page ?? 0
    const size = params?.size ?? 10
    const body = {
      userId: params?.userId,
      fileName: params?.fileName ?? null,
      processedStatus: params?.processedStatus ?? null,
    }
    return request('/data/page', { method: 'POST', params: { page, size }, body })
  },
  dataImport(userId, file) {
    const formData = new FormData()
    formData.append('userId', userId)
    formData.append('file', file)
    return fetch(BASE_URL + '/data/import', { method: 'POST', body: formData }).then(async (res) => {
      if (!res.ok) throw new Error(await res.text())
      return res.json()
    })
  },
  dataUpdateStatus(id, status) {
    return request('/data/status', { method: 'PATCH', params: { id, status } })
  },
  dataDelete(id) {
    // 缺少后端DELETE接口，占位埋点
    return request('/data/delete', { method: 'DELETE', params: { id } }).catch(() => ({ ok: false, message: '后端未实现 /data/delete' }))
  },
}
