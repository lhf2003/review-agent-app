import forge from 'node-forge'
import { ElMessage } from 'element-plus'
const isDev = typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.DEV
const isEmbeddedHttp = typeof window !== 'undefined' && window.location && window.location.protocol === 'http:' && window.location.port === '3000'
const BASE_URL = "/api"
const AES_KEY_STR = 'ReviewAgentSecureKey20250101!!!!';

async function encryptPassword(password) {
  if (!password) return password;
  if (window.crypto && window.crypto.subtle) {
    try {
      const enc = new TextEncoder();
      const keyMaterial = await window.crypto.subtle.importKey(
        'raw',
        enc.encode(AES_KEY_STR),
        { name: 'AES-GCM' },
        false,
        ['encrypt']
      );
      const iv = window.crypto.getRandomValues(new Uint8Array(12));
      const encodedPassword = enc.encode(password);
      const ciphertext = await window.crypto.subtle.encrypt(
        { name: 'AES-GCM', iv },
        keyMaterial,
        encodedPassword
      );
      const combined = new Uint8Array(iv.length + ciphertext.byteLength);
      combined.set(iv);
      combined.set(new Uint8Array(ciphertext), iv.length);
      let binary = '';
      const bytes = combined;
      const len = bytes.byteLength;
      for (let i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
      }
      return window.btoa(binary);
    } catch (e) {
      console.warn('WebCrypto AES-GCM failed, falling back to forge', e);
    }
  }
  try {
    const ivRaw = forge.random.getBytesSync(12);
    const cipher = forge.cipher.createCipher('AES-GCM', AES_KEY_STR);
    cipher.start({ iv: ivRaw, tagLength: 128 });
    cipher.update(forge.util.createBuffer(password, 'utf8'));
    const ok = cipher.finish();
    if (!ok) throw new Error('forge cipher.finish() failed');
    const ctRaw = cipher.output.getBytes();
    const tagRaw = cipher.mode.tag.getBytes();
    const combinedRaw = ivRaw + ctRaw + tagRaw;
    return window.btoa(combinedRaw);
  } catch (e) {
    console.error('Encryption fallback failed', e);
    throw new Error('加密不可用：请使用HTTPS或更新浏览器');
  }
}

function getToken() {
  try {
    const token = localStorage.getItem('token')
    return token || null
  } catch (e) {
    console.error('Failed to get token from localStorage', e)
  }
  return null
}

// 保留 getUserId 用于获取用户 ID（但不用于认证）
function getUserId() {
  try {
    const authRaw = localStorage.getItem('auth')
    if (authRaw) {
      const auth = JSON.parse(authRaw)
      return auth.userId
    }
  } catch (e) {
    console.error('Failed to parse auth from localStorage', e)
  }
  return null
}

async function request(path, { method = 'GET', params, body, headers } = {}) {
  // 统一添加 /api 前缀，触发代理
  const baseUrl = '/api'
  let url = path.startsWith('http') ? path : (baseUrl + path)

  if (params) {
    // 过滤掉 null 和 undefined 值，避免 URLSearchParams 将其转换为 "null" 字符串
    const cleanParams = Object.fromEntries(
      Object.entries(params).filter(([_, v]) => v != null)
    )
    const usp = new URLSearchParams(cleanParams)
    url += `?${usp.toString()}`
  }

  // 获取 Token（用于 JWT 认证）
  const token = getToken()

  const finalHeaders = {
    'Content-Type': 'application/json',
    ...(headers || {}),
  }

  // 只添加 Authorization Header（JWT），后端通过 SecurityContext 获取用户 ID
  if (token) {
    finalHeaders['Authorization'] = `Bearer ${token}`
  }

  const res = await fetch(url, {
    method,
    headers: finalHeaders,
    body: body ? JSON.stringify(body) : undefined,
  })

  // 处理错误响应
  if (!res.ok) {
    const text = await res.text()

    // 尝试解析 JSON，提取 message 字段
    let errorMsg = ''
    try {
      const json = JSON.parse(text)
      errorMsg = json.message || json.msg || `HTTP ${res.status} 错误`
    } catch (e) {
      // JSON 解析失败，使用原始文本
      errorMsg = text || `HTTP ${res.status} 错误`
    }

    // 401 未授权：Token 无效或过期
    if (res.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('auth')
      ElMessage.error('登录已过期，请重新登录')
      // 跳转到登录页
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      throw new Error('未授权，请重新登录')
    }

    // 429 请求过于频繁
    if (res.status === 429) {
      ElMessage.error('请求过于频繁，请稍后再试')
      throw new Error('请求过于频繁')
    }

    // 直接抛出错误，不在这里显示（让调用方决定如何处理）
    throw new Error(errorMsg)
  }

  const data = await res.json().catch(() => null)
  return normalizeResponse(data)
}

export const api = {
  // auth
  async login(username, password) {
    const encryptedPassword = await encryptPassword(password)
    const response = await request('/user/login', { method: 'POST', body: { username, password: encryptedPassword } })

    // 保存 Token 和用户信息
    if (response && response.token) {
      localStorage.setItem('token', response.token)
      // 保存用户信息到 auth（兼容现有代码）
      const auth = {
        userId: response.userInfo?.id,
        username: response.userInfo?.username
      }
      localStorage.setItem('auth', JSON.stringify(auth))
    }

    return response
  },
  async register(username, password) {
    const encryptedPassword = await encryptPassword(password)
    return request('/user/register', { method: 'POST', body: { username, password: encryptedPassword } })
  },

  // 退出登录
  logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('auth')
    if (typeof window !== 'undefined') {
      window.location.href = '/login'
    }
  },

  // config
  getConfig() {
    return request('/user/config/get')
  },
  updateConfig(body) {
    return request('/user/config/update', { method: 'POST', body })
  },
  getUserModelConfig() {
    return request('/user/config/model/get')
  },
  updateUserModelConfig(userId, list) {
    return request('/user/config/model/update', { method: 'POST', body: list })
  },

  // tag controller endpoints
  getMainTagList() {
    return request('/tag/list')
  },
  getTagRelations(mainTagId) {
    const params = mainTagId != null ? { mainTagId } : undefined
    return request('/tag/list/relation', { params })
  },
  addMainTag(mainTag) {
    return request('/tag/add', { method: 'POST', body: mainTag })
  },
  updateMainTag(mainTag) {
    return request('/tag/update', { method: 'POST', body: mainTag })
  },
  deleteMainTag(id) {
    return request('/tag/delete', { method: 'DELETE', params: { id } })
  },
  getSubTagList() {
    return request('/tag/sub/list')
  },
  addSubTag(subTag) {
    return request('/tag/add/sub', { method: 'POST', body: subTag })
  },
  updateSubTag(subTag) {
    return request('/tag/update/sub', { method: 'POST', body: subTag })
  },
  deleteSubTag(id) {
    return request('/tag/delete/sub', { method: 'DELETE', params: { id } })
  },
  addTagRelation(params) {
    return request('/tag/add/relation', { method: 'POST', body: params })
  },
  addRecommendTag(body) {
    return request('/tag/recommand/add', { method: 'POST', body })
  },
  deleteTagRelation(params) {
    return request('/tag/delete/relation', { method: 'DELETE', body: params })
  },

  // file info
  importFile(file) {
    const formData = new FormData()
    formData.append('file', file)

    // 只传递 Authorization Header（JWT），后端通过 SecurityContext 获取用户 ID
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
  updateFileStatus(id, status) {
    return request('/file-info/status', { method: 'PATCH', params: { id, status } })
  },

  // sync history
  getSyncHistory() {
    return request('/sync-record/history')
  },

  // user info
  getUserInfo(id) {
    return request('/user/info', { params: { id } })
  },
  updateUserInfo(body) {
    return request('/user/info/update', { method: 'POST', body })
  },
  async updateUserPassword(oldPassword, newPassword) {
    const encOld = await encryptPassword(oldPassword)
    const encNew = await encryptPassword(newPassword)
    return request('/user/info/update/password', { method: 'POST', body: { oldPassword: encOld, newPassword: encNew } })
  },
  getUserStats() {
    return request('/user/stats')
  },

  getAnalysisList(params) {
    const page = params?.page ?? 0
    const size = params?.size ?? 10
    
    const body = {
        userId: getUserId(),
        ...(params || {})
    }
    // Remove page/size from body if they are in params
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
  getAnalysisByFile(fileId) {
    // 预期：返回指定 fileId 的分析列表
    return request('/analysis/by-file', { params: { fileId } }).catch(() => ({
      list: [
        { id: 11, title: '示例详情', problemStatement: '示例问题详情', rootCause: '示例根因', mastery: 50, tags: ['Demo'], date: '2025-11-29' },
      ],
    }))
  },
  startAnalysis(fileId) {
    return request('/analysis/start', { method: 'GET', params: { fileId } })
  },
  getAnalysisResult(dataId) {
    return request('/analysis/result', { params: { dataId } })
  },
  // 获取分析结果（需要同时传 dataId 与 analysisId）
  getAnalysisResultByIds(dataId, analysisId) {
    return request('/analysis/result', { params: { dataId, analysisId } })
  },
  // session trace
  getSessionTrace(fileId) {
    return request('/session-trace/get', { params: { fileId } })
  },
  getFileNameList() {
    return request('/analysis/file-name/list')
  },
  getTagStats() {
    // 返回 { tags: [{ id, name, count }] }
    return request('/analysis/tag/list')
  },
  getSimilarIssues(analysisResultId) {
    return request('/analysis/similarity', { params: { analysisId: analysisResultId } })
      .then((resp) => resp?.data || resp)
  },

  // data page (DataInfo)
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
  dataCreate(body) {
    return request('/data/create', { method: 'POST', body })
  },
  dataImport(userId, file, source) {
    const formData = new FormData()
    formData.append('file', file)
    if (source !== undefined && source !== null) formData.append('source', source)

    // 只传递 Authorization Header（JWT），后端通过 SecurityContext 获取用户 ID
    const token = getToken()
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`

    return fetch(BASE_URL + '/data/import', { method: 'POST', headers, body: formData }).then(async (res) => {
      if (!res.ok) throw new Error(await res.text())
      const json = await res.json()
      return normalizeResponse(json)
    })
  },
  dataUpdateStatus(id, status) {
    return request('/data/status', { method: 'PATCH', params: { id, status } })
  },
  dataDelete(id) {
    return request('/data/delete', { method: 'DELETE', params: { id } })
  },

  // report
  getWordReport(startDate, endDate) {
    return request('/statistic/word-cloud', {
      method: 'POST',
      body: { startDate, endDate }
    })
      .then((resp) => resp?.data || resp)
      .catch(() => ({ 并发: 2, Java: 1, 性能优化: 2, 基础语法: 2, SQL: 1 }))
  },

  // statistic
  getDateTagCountTrend(startDate, endDate) {
    return request('/statistic/tag/trend', {
      method: 'POST',
      body: { startDate, endDate }
    })
  },
  getReportList(type = 1, date) {
    const params = { type }
    if (date) params.date = date
    return request('/report/get', { params })
  },

  // llm model
  connectLlmProvider(provider) {
    return request('/llm/model/connect', { method: 'POST', body: provider })
  },
  getLlmModels(provider) {
    return request('/llm/model/list', { method: 'POST', body: provider })
  },
  
  // User Selected Models
  getSelectedModelList(providerId) {
    return request('/user/config/model/list', { method: 'POST', params: { providerId } })
  },
  activeSelectedModel(selectedModel) {
    return request('/user/config/model/active', { method: 'POST', body: selectedModel })
  },
  deactiveSelectedModel(selectedModel) {
    return request('/user/config/model/deactive', { method: 'POST', body: selectedModel })
  },

  // User Default Model Config
  getUserDefaultModels() {
    return request('/user/config/default-model/get')
  },
  updateUserDefaultModels(modelConfigs) {
    return request('/user/config/default-model/update', { method: 'POST', body: modelConfigs })
  },

  _handleStream(fetchPromise, handlers) {
    return fetchPromise
      .then((res) => {
        if (!res.ok) throw new Error('HTTP ' + res.status)
        if (handlers.onOpen) handlers.onOpen()
        const reader = res.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''
        function pump() {
          return reader.read().then(({ done, value }) => {
            if (done) {
              if (handlers.onDone) handlers.onDone()
              return
            }
            buffer += decoder.decode(value, { stream: true })
            const parts = buffer.split('\n\n')
            buffer = parts.pop() || ''
            for (const part of parts) {
              const lines = part.split('\n')

              // 解析事件类型
              let eventType = null
              const eventLines = lines.filter((l) => l.startsWith('event:'))
              if (eventLines.length) {
                const eventContent = eventLines[0].slice(6).trim()
                eventType = eventContent || null
              }

              // 解析数据
              const dataLines = lines.filter((l) => l.startsWith('data:'))
              if (dataLines.length) {
                const data = dataLines.map((l) => {
                  let content = l.slice(5)
                  if (content.startsWith(' ')) {
                    content = content.slice(1)
                  }
                  return content
                }).join('\n')

                // 根据事件类型调用不同的处理函数
                if (eventType === 'error' && handlers.onErrorEvent) {
                  handlers.onErrorEvent(data)
                } else if (eventType === 'stage' && handlers.onStage) {
                  handlers.onStage(data)
                } else if (handlers.onEvent) {
                  // 向后兼容：没有事件类型时使用 onEvent
                  handlers.onEvent(data)
                }
              }
            }
            return pump()
          })
        }
        return pump()
      })
      .catch((err) => {
        if (handlers.onError) handlers.onError(err)
      })
  },

  chatStream(requestText, handlers = {}) {
    const controller = new AbortController()
    // 只传递 Authorization Header（JWT），后端通过 SecurityContext 获取用户 ID
    const token = getToken()
    const headers = { Accept: 'text/event-stream' }
    if (token) headers['Authorization'] = `Bearer ${token}`

    const url = new URL('/chat')
    url.searchParams.set('request', requestText || '')

    const p = fetch(url.toString(), { method: 'GET', headers, signal: controller.signal })
    this._handleStream(p, handlers)
    return { cancel: () => controller.abort() }
  },

  chatWithAnalysisStream(requestText, handlers = {}) {
    const controller = new AbortController()
    // 只传递 Authorization Header（JWT），后端通过 SecurityContext 获取用户 ID
    const token = getToken()
    const headers = { Accept: 'text/event-stream' }
    if (token) headers['Authorization'] = `Bearer ${token}`

    const url = new URL(BASE_URL + '/chat/with-analysis')
    url.searchParams.set('request', requestText || '')

    const p = fetch(url.toString(), { method: 'POST', headers, signal: controller.signal })
    this._handleStream(p, handlers)
    return { cancel: () => controller.abort() }
  },
  
  // 埋点：获取聊天框轮播消息列表
  getChatPlaceholders() {
    return request('/chat/placeholders').catch(() => [
      '你需要我的帮助吗？',
      '发现一个新文件，需要我分析吗？',
      '输入关键字搜索分析结果...',
      '试试问我关于代码的问题'
    ])
  },
 
   analysisLogStream(handlers = {}) {
      const controller = new AbortController()
      // 只传递 Authorization Header（JWT），后端通过 SecurityContext 获取用户 ID
      const token = getToken()
      const headers = { Accept: 'text/event-stream' }
      if (token) headers['Authorization'] = `Bearer ${token}`

      const p = fetch(BASE_URL + '/analysis/log/stream', { method: 'GET', headers, signal: controller.signal })
      this._handleStream(p, handlers)
      return { cancel: () => controller.abort() }
    },
   
  // --- Collection API ---
  getCollectionList() {
    return request('/collection/list')
      .then(data => ({ list: Array.isArray(data) ? data : [] }))
      .catch(() => ({
      list: [
        { id: 1, name: 'React 性能优化', description: '关于 React 渲染与 Hooks 的常见问题', count: 5, updatedAt: '2025-10-01' },
        { id: 2, name: '后端并发编程', description: 'Java 线程池与锁机制', count: 3, updatedAt: '2025-09-20' }
      ]
    }))
  },
  createCollection(data) {
    return request('/collection/add', { method: 'POST', body: data })
      .then(res => (typeof res === 'number' || typeof res === 'string' ? { id: res } : res))
  },
  updateCollection(id, data) {
    return request('/collection/update', { method: 'PUT', body: data, params: { id } })
  },
  deleteCollection(id) {
    return request('/collection/delete', { method: 'DELETE', params: { id } })
  },
  getCollectionDetail(id) {
    return request('/collection/detail', { params: { id } })
  },
  addSessionToCollection(data) {
    // data: { collectionId, sessionId }
    const body = { action: 'ADD', analysisIds: [data.sessionId] }
    return request('/collection/items', { method: 'POST', body, params: { collectionId: data.collectionId } })
  },
  removeSessionFromCollection(collectionId, sessionId) {
    const body = { action: 'REMOVE', analysisIds: [sessionId] }
    return request('/collection/items', { method: 'POST', body, params: { collectionId } })
  },
  generateQuiz(collectionId) {
    return request('/collection/generate-quiz', { method: 'POST', body: { collectionId } })
  },
  submitAnswer(questionId, userAnswer) {
    return request('/collection/submit-answer', { method: 'POST', body: { questionId, userAnswer } })
  },
  submitBatchAnswers(quizId, answers) {
    // answers: [{ questionId, userAnswer }, ...]
    return request('/collection/submit-batch-answers', { method: 'POST', body: { quizId, answers } })
  },
  resetQuiz(quizId) {
    return request('/collection/reset', { method: 'POST', body: { quizId } })
  },

  // --- Mistake Book API ---
  getMistakeList(filter = 'all') {
    return request('/mistake-book/list', { params: { filter } })
      .then(data => Array.isArray(data) ? data : [])
  },
  getMistakeStats() {
    return request('/mistake-book/stats')
  },
  /**
   * 获取复习推荐列表
   * 基于遗忘曲线算法返回需要复习的错题
   */
  getReviewRecommendation() {
    return request('/mistake-book/review-recommendation')
      .then(data => {
        // 调试日志：查看返回的数据
        console.log('[getReviewRecommendation] 原始响应:', data)
        console.log('[getReviewRecommendation] 是否为数组:', Array.isArray(data))
        console.log('[getReviewRecommendation] 数据类型:', typeof data)
        const result = Array.isArray(data) ? data : []
        console.log('[getReviewRecommendation] 最终结果:', result)
        return result
      })
      .catch(error => {
        console.error('[getReviewRecommendation] 请求失败:', error)
        return []
      })
  },
  markMistakesMastered(questionIds) {
    return request('/mistake-book/mark-mastered', { method: 'POST', body: { questionIds } })
  },
  deleteMistakes(questionIds) {
    return request('/mistake-book/delete', { method: 'DELETE', body: { questionIds } })
  },

  // ========== Quiz API ==========
  /**
   * 获取习题历史
   */
  getQuizHistory(params) {
    return request('/collection/quiz/history', { params })
  },

  /**
   * 获取习题详情
   */
  getQuizDetail(quizId) {
    return request(`/collection/quiz/${quizId}/detail`)
  },

  /**
   * 检测题库版本
   */
  checkQuizVersion(collectionId) {
    return request(`/collection/${collectionId}/quiz/version-check`)
  },

  /**
   * 重新生成题库
   */
  regenerateQuiz(collectionId) {
    return request(`/collection/${collectionId}/quiz/regenerate`, { method: 'POST' })
  },

  /**
   * 获取习题统计数据
   */
  getQuizStats() {
    return request('/collection/quiz/stats')
  },

  /**
   * 获取合集推荐列表（基于薄弱知识点）
   */
  getCollectionRecommendations(limit = 8) {
    return request('/collection/recommendations', { params: { limit } })
      .then(data => Array.isArray(data) ? data : [])
      .catch(() => [])
  },

  /**
   * 获取知识点掌握度列表
   */
  getKnowledgeMastery(limit = 20) {
    return request('/collection/quiz/knowledge-mastery', { params: { limit } })
      .then(data => Array.isArray(data) ? data : [])
      .catch(() => [])
  }
}

function normalizeResponse(resp) {
  if (resp == null) return resp
  const hasCode = Object.prototype.hasOwnProperty.call(resp, 'code')
  if (!hasCode) return resp
  const codeNum = Number(resp.code)
  if (Number.isNaN(codeNum)) return resp
  if (codeNum === 0) {
    return resp.data !== undefined ? resp.data : resp
  }
  // 只提取错误信息，不显示（让调用方决定如何处理）
  const msg = resp.message || resp.msg || '请求失败'
  throw new Error(msg)
}
