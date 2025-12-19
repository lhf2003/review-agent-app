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
  const hasUnreadMessage = ref(false)
  let streamCtl = null

  function open() { 
    isOpen.value = true 
    hasUnreadMessage.value = false
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
    isOpen.value = false // Don't open immediately
    hasUnreadMessage.value = true // Trigger glow
    
    // Auto-send context
    const contextMsg = `我想讨论这个分析结果：\n\n**问题**：${card.problem}\n\n**根因/方案**：${card.rootCause}`
    
    // Manually push messages to UI
    messages.value.push({ role: 'user', content: contextMsg })
    messages.value.push({ role: 'assistant', content: '我已理解您的问题，可以随时向我提问' })
    
    if (!auth.userId) { 
        ElMessage.warning('请先登录')
        return 
    }
    
    // Send to backend to establish context, but ignore response (fire and forget)
    api.chatWithAnalysisStream(contextMsg, {
      onEvent: () => {}, 
      onDone: () => {},
      onError: (e) => { console.error('Background context sync failed', e) }
    })
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
    hasUnreadMessage,
    open,
    close,
    toggleFullScreen,
    startAnalysisChat,
    sendMessage,
    reset
  }
})
