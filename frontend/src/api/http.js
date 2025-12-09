const isDev = typeof import.meta !== 'undefined' && import.meta.env && import.meta.env.DEV
const isEmbeddedHttp = typeof window !== 'undefined' && window.location && window.location.protocol === 'http:' && window.location.port === '3000'
// const BASE_URL = isDev || isEmbeddedHttp ? '/' : 'http://localhost:8081'
const BASE_URL = 'http://localhost:8081'
const AES_KEY_STR = 'ReviewAgentSecureKey20250101!!!!';

async function encryptPassword(password) {
  if (!password) return password;
  try {
    const enc = new TextEncoder();
    const keyMaterial = await window.crypto.subtle.importKey(
      "raw",
      enc.encode(AES_KEY_STR),
      "AES-GCM",
      false,
      ["encrypt"]
    );
    
    const iv = window.crypto.getRandomValues(new Uint8Array(12));
    const encodedPassword = enc.encode(password);
    
    const ciphertext = await window.crypto.subtle.encrypt(
      {
        name: "AES-GCM",
        iv: iv
      },
      keyMaterial,
      encodedPassword
    );
    
    // Combine IV + Ciphertext
    const combined = new Uint8Array(iv.length + ciphertext.byteLength);
    combined.set(iv);
    combined.set(new Uint8Array(ciphertext), iv.length);
    
    // Convert to Base64
    let binary = '';
    const bytes = combined;
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return window.btoa(binary);
  } catch (e) {
    console.error('Encryption failed', e);
    return password;
  }
}

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
  let url = BASE_URL + path
  if (params) {
    const usp = new URLSearchParams(params)
    url += `?${usp.toString()}`
  }
  
  const userId = getUserId()
  const finalHeaders = {
    'Content-Type': 'application/json',
    ...(headers || {}),
  }
  if (userId) {
    finalHeaders['userId'] = userId
  }

  const res = await fetch(url, {
    method,
    headers: finalHeaders,
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
  // auth
  async login(username, password) {
    const encryptedPassword = await encryptPassword(password)
    return request('/user/login', { method: 'POST', body: { username, password: encryptedPassword } })
  },
  async register(username, password) {
    const encryptedPassword = await encryptPassword(password)
    return request('/user/register', { method: 'POST', body: { username, password: encryptedPassword } })
  },

  // config
  getConfig() {
    return request('/user/config/get')
  },
  updateConfig(body) {
    return request('/user/config/update', { method: 'POST', body })
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
  deleteTagRelation(params) {
    return request('/tag/delete/relation', { method: 'DELETE', body: params })
  },

  // file info
  importFile(file) {
    const formData = new FormData()
    formData.append('file', file)
    
    const userId = getUserId()
    const headers = {}
    if (userId) {
      headers['userId'] = userId
    }

    return fetch(BASE_URL + '/file-info/import', {
      method: 'POST',
      headers: headers,
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
  getTagStats(params) {
    // 返回 { tags: [{ id, name, count }] }
    return request('/analysis/tag/list', { params }).catch(() => ({ tags: [{ id: 1, name: 'React', count: 8 }, { id: 2, name: 'useEffect', count: 5 }, { id: 3, name: 'Python', count: 6 }, { id: 4, name: '错误处理', count: 4 }] }))
  },

  // data page (DataInfo)
  dataPage(params) {
    const page = params?.page ?? 0
    const size = params?.size ?? 10
    const body = {
      userId: getUserId(),
      fileName: params?.fileName ?? null,
      processedStatus: params?.processedStatus ?? null,
    }
    return request('/data/page', { method: 'POST', params: { page, size }, body })
  },
  dataImport(file) {
    const formData = new FormData()
    formData.append('file', file)
    
    const userId = getUserId()
    const headers = {}
    if (userId) {
      headers['userId'] = userId
    }

    return fetch(BASE_URL + '/data/import', { method: 'POST', headers, body: formData }).then(async (res) => {
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

  // report
  getWordReport() {
    return request('/report/word')
      .then((resp) => resp?.data || resp)
      .catch(() => ({ 并发: 2, Java: 1, 性能优化: 2, 基础语法: 2, SQL: 1 }))
  },
}
