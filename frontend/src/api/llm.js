/**
 * LLM 模型相关 API
 * 包含模型连接、模型配置、聊天等功能
 */
import { request, handleStream, getToken, BASE_URL } from './base'

export const llmApi = {
  // ========== 模型连接 ==========

  /**
   * 连接 LLM 提供商
   */
  connectLlmProvider(provider) {
    return request('/llm/model/connect', { method: 'POST', body: provider })
  },

  /**
   * 获取 LLM 模型列表
   */
  getLlmModels(provider) {
    return request('/llm/model/list', { method: 'POST', body: provider })
  },

  // ========== 用户模型配置 ==========

  /**
   * 获取用户选择的模型列表
   */
  getSelectedModelList(providerId) {
    return request('/user/config/model/list', { method: 'POST', params: { providerId } })
  },

  /**
   * 激活选择的模型
   */
  activeSelectedModel(selectedModel) {
    return request('/user/config/model/active', { method: 'POST', body: selectedModel })
  },

  /**
   * 停用选择的模型
   */
  deactiveSelectedModel(selectedModel) {
    return request('/user/config/model/deactive', { method: 'POST', body: selectedModel })
  },

  // ========== 默认模型配置 ==========

  /**
   * 获取用户默认模型配置
   */
  getUserDefaultModels() {
    return request('/user/config/default-model/get')
  },

  /**
   * 更新用户默认模型配置
   */
  updateUserDefaultModels(modelConfigs) {
    return request('/user/config/default-model/update', { method: 'POST', body: modelConfigs })
  },

  // ========== 聊天功能 ==========

  /**
   * 聊天占位符消息列表
   */
  getChatPlaceholders() {
    return request('/chat/placeholders').catch(() => [
      '你需要我的帮助吗？',
      '发现一个新文件，需要我分析吗？',
      '输入关键字搜索分析结果...',
      '试试问我关于代码的问题'
    ])
  },

  /**
   * 聊天流式请求
   */
  chatStream(requestText, handlers = {}) {
    const controller = new AbortController()
    const token = getToken()
    const headers = { Accept: 'text/event-stream' }
    if (token) headers['Authorization'] = `Bearer ${token}`

    // 使用相对路径，让 Vite proxy 生效
    const url = `/chat?request=${encodeURIComponent(requestText || '')}`

    const p = fetch(url, { method: 'GET', headers, signal: controller.signal })
    handleStream(p, handlers)
    return { cancel: () => controller.abort() }
  },

  /**
   * 带分析的聊天流式请求
   */
  chatWithAnalysisStream(requestText, handlers = {}) {
    const controller = new AbortController()
    const token = getToken()
    const headers = { Accept: 'text/event-stream' }
    if (token) headers['Authorization'] = `Bearer ${token}`

    // 使用相对路径，让 Vite proxy 生效
    // 注意：后端端点是 /chat/with-analysis，不是 /api/chat/with-analysis
    const url = `/chat/with-analysis?request=${encodeURIComponent(requestText || '')}`

    const p = fetch(url, { method: 'POST', headers, signal: controller.signal })
    handleStream(p, handlers)
    return { cancel: () => controller.abort() }
  }
}

export default llmApi
