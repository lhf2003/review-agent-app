import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useAuthStore } from './auth'
import { ElMessage } from 'element-plus'
import { ChatStreamHandler } from '../utils/chat/ChatStreamHandler.js'

export const useChatStore = defineStore('chat', () => {
  // ==================== 依赖注入 ====================
  const auth = useAuthStore()

  // ==================== 状态定义 ====================
  const isOpen = ref(false)
  const isFullScreen = ref(false)
  const mode = ref('normal') // 'normal' | 'analysis'
  const hasUnreadMessage = ref(false)
  const messages = ref([])
  const isStreaming = ref(false)
  let currentController = null

  // ==================== 计算属性 ====================

  // ==================== API 调用函数 ====================
  async function callApiDirect(content, signal) {
    const token = localStorage.getItem('token')
    const headers = {
      'Accept': 'text/event-stream',
      'Content-Type': 'application/json'
    }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    const url = mode.value === 'analysis'
      ? `/chat/with-analysis?request=${encodeURIComponent(content)}`
      : `/chat?request=${encodeURIComponent(content)}`
    const method = mode.value === 'analysis' ? 'POST' : 'GET'

    const response = await fetch(url, { method, headers, signal })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    return response
  }

  // ==================== 核心方法 ====================

  /**
   * 打开聊天窗口
   */
  function open() {
    isOpen.value = true
    hasUnreadMessage.value = false
  }

  /**
   * 关闭聊天窗口
   */
  function close() {
    isOpen.value = false
  }

  /**
   * 切换全屏模式
   */
  function toggleFullScreen() {
    isFullScreen.value = !isFullScreen.value
  }

  /**
   * 开始分析模式聊天
   */
  function startAnalysisChat(card) {
    mode.value = 'analysis'
    reset()

    isOpen.value = false
    hasUnreadMessage.value = true

    const contextMsg = `我想讨论这个分析结果：\n\n**问题**：${card.problem}\n\n**根因/方案**：${card.rootCause}`

    // 添加消息到 UI
    messages.value = [
      { id: `analysis-${Date.now()}`, content: contextMsg, role: 'user' },
      { id: `analysis-ai-${Date.now()}`, content: '我已理解您的问题，可以随时向我提问', role: 'assistant' }
    ]

    if (!auth.userId) {
      ElMessage.warning('请先登录')
      return
    }

    // 发送到后端建立上下文
    callApiDirect(contextMsg).catch(() => {})
  }

  /**
   * 发送消息
   */
  async function sendMessage(text) {
    if (!auth.userId) {
      ElMessage.warning('请先登录')
      return
    }

    if (!text || !text.trim()) {
      return
    }

    const content = text.trim()

    // 添加用户消息
    messages.value.push({ id: Date.now().toString(), content, role: 'user' })

    // 添加 AI 占位消息
    const aiMsgIndex = messages.value.length
    messages.value.push({ id: (Date.now() + 1).toString(), content: '', role: 'assistant' })

    isStreaming.value = true

    try {
      const streamHandler = new ChatStreamHandler()
      const response = await callApiDirect(content, streamHandler.controller.signal)

      currentController = streamHandler
      let buffer = ''

      await new Promise((resolve, reject) => {
        streamHandler.on('chunk', (data) => {
          if (typeof data === 'string') {
            buffer += data
          } else if (data?.content) {
            buffer += data.content
          } else if (data?.text) {
            buffer += data.text
          }
          // 更新消息内容（创建新对象触发 Vue 响应式）
          messages.value[aiMsgIndex] = { ...messages.value[aiMsgIndex], content: buffer }
        })

        streamHandler.on('complete', () => {
          resolve()
        })

        streamHandler.on('error', (error) => {
          reject(error)
        })

        streamHandler.start(response)
      })
    } catch (error) {
      messages.value[aiMsgIndex] = { ...messages.value[aiMsgIndex], content: '发送失败，请稍后重试' }
    } finally {
      isStreaming.value = false
      currentController = null
    }
  }

  /**
   * 重置聊天
   */
  function reset() {
    if (currentController) {
      currentController.abort()
      currentController = null
    }
    mode.value = 'normal'
    messages.value = []
    isStreaming.value = false
  }

  /**
   * 清空上下文
   */
  async function clearContext() {
    try {
      const token = localStorage.getItem('token')
      await fetch('/chat/clear', {
        method: 'GET',
        headers: { 'Authorization': `Bearer ${token}` }
      })
      reset()
      ElMessage.success('已清空对话上下文')
    } catch (error) {
      ElMessage.error('清空失败')
    }
  }

  // ==================== 导出 ====================
  return {
    // 状态
    isOpen,
    isFullScreen,
    messages,
    isStreaming,
    mode,
    hasUnreadMessage,

    // 方法
    open,
    close,
    toggleFullScreen,
    startAnalysisChat,
    sendMessage,
    reset,
    clearContext
  }
})
