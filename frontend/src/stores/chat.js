import { defineStore } from 'pinia'
import { ref } from 'vue'
import { api } from '../api/http'
import { useAuthStore } from './auth'
import { ElMessage } from 'element-plus'

export const useChatStore = defineStore('chat', () => {
  const auth = useAuthStore()
  const isOpen = ref(false)
  const isFullScreen = ref(false)
  const messages = ref([])
  const isStreaming = ref(false)
  const mode = ref('normal') // 'normal' | 'analysis'
  let streamCtl = null

  function open() { 
    isOpen.value = true 
  }
  
  function close() { 
    isOpen.value = false 
    if (streamCtl && isStreaming.value) {
      streamCtl.cancel()
      isStreaming.value = false
    }
  }
  
  function toggleFullScreen() { isFullScreen.value = !isFullScreen.value }

  function startAnalysisChat(card) {
    mode.value = 'analysis'
    messages.value = [] // Clear previous messages
    isOpen.value = true
    
    // Auto-send context
    const contextMsg = `我想讨论这个分析结果：\n\n**问题**：${card.problem}\n\n**根因/方案**：${card.rootCause}`
    
    sendMessage(contextMsg)
  }
  
  function sendMessage(text) {
    if (!auth.userId) { ElMessage.warning('请先登录'); return }
    if (!text) return
    
    // Avoid pushing duplicate user message if called from startAnalysisChat where we might want to control it
    // But here we just assume sendMessage adds to UI and sends.
    messages.value.push({ role: 'user', content: text })
    _sendToApi(text)
  }

  function _sendToApi(text) {
    isStreaming.value = true
    
    const handler = {
      onEvent: (chunk) => {
        const lastMsg = messages.value[messages.value.length - 1]
        if (!lastMsg || lastMsg.role !== 'assistant') {
          messages.value.push({ role: 'assistant', content: '' })
        }
        messages.value[messages.value.length - 1].content += chunk
      },
      onDone: () => { isStreaming.value = false },
      onError: (e) => { 
        isStreaming.value = false; 
        ElMessage.error('聊天失败: ' + (e?.message || e)) 
      }
    }

    if (mode.value === 'analysis') {
      streamCtl = api.chatWithAnalysisStream(text, handler)
    } else {
      streamCtl = api.chatStream(text, handler)
    }
  }

  function reset() {
    mode.value = 'normal'
    messages.value = []
  }
  
  return {
    isOpen,
    isFullScreen,
    messages,
    isStreaming,
    mode,
    open,
    close,
    toggleFullScreen,
    startAnalysisChat,
    sendMessage,
    reset
  }
})
