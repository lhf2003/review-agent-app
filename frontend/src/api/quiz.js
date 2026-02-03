/**
 * 习题相关API
 */

// 获取 Token
function getToken() {
  try {
    const token = localStorage.getItem('token')
    return token || null
  } catch (e) {
    console.error('Failed to get token from localStorage', e)
  }
  return null
}

// 统一请求函数
async function request(path, { method = 'GET', params, body } = {}) {
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

  const token = getToken()

  const finalHeaders = {
    'Content-Type': 'application/json',
  }

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
      errorMsg = text || `HTTP ${res.status} 错误`
    }

    // 401 未授权：Token 无效或过期
    if (res.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('auth')
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
      throw new Error('未授权，请重新登录')
    }

    throw new Error(errorMsg)
  }

  const data = await res.json().catch(() => null)
  return normalizeResponse(data)
}

// 响应标准化
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

export default {
  /**
   * 获取习题历史
   * @param {Object} params - 查询参数
   * @param {number} params.status - 状态筛选（null=全部，0=进行中，1=已完成）
   * @param {number} params.collectionId - 合集筛选（null=全部合集）
   * @param {number} params.page - 页码（从0开始）
   * @param {number} params.size - 每页大小
   * @returns {Promise} 分页的习题历史
   */
  getQuizHistory(params) {
    return request('/quiz/history', { params })
  },

  /**
   * 获取习题详情
   * @param {number} quizId - 习题ID
   * @returns {Promise} 习题详情
   */
  getQuizDetail(quizId) {
    return request(`/quiz/${quizId}/detail`)
  },

  /**
   * 检测题库版本
   * @param {number} collectionId - 合集ID
   * @returns {Promise} 版本检测结果
   */
  checkQuizVersion(collectionId) {
    return request(`/collection/${collectionId}/quiz/version-check`)
  },

  /**
   * 重新生成题库
   * @param {number} collectionId - 合集ID
   * @returns {Promise} 新生成的QuizRecord
   */
  regenerateQuiz(collectionId) {
    return request(`/collection/${collectionId}/quiz/regenerate`, { method: 'POST' })
  },

  /**
   * 获取习题统计数据
   * @returns {Promise} 习题统计数据（分数趋势和知识点掌握度）
   */
  getQuizStats() {
    return request('/quiz/stats')
  }
}
