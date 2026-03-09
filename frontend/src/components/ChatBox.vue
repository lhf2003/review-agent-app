<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { FullScreen, Delete, Close, ChatLineRound, Cpu, ChatDotRound } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { useChatStore } from '../stores/chat'
import MarkdownRenderer from './MarkdownRenderer.vue'

const auth = useAuthStore()
const chatStore = useChatStore()
const chatInput = ref('')

// Placeholder messages
const placeholderMessages = ref([
  '你需要我的帮助吗？',
  '发现一个新文件，需要我分析吗？',
  '输入关键字搜索分析结果...',
  '试试问我关于代码的问题'
])
const currentPlaceholderIndex = ref(0)
let placeholderInterval = null

const displayText = computed(() => {
  if (chatStore.hasUnreadMessage && chatStore.messages.length > 0) {
    const lastMsg = chatStore.messages[chatStore.messages.length - 1]
    if (lastMsg && lastMsg.role === 'assistant') {
      return lastMsg.content
    }
    return '收到一条新消息'
  }
  return placeholderMessages.value[currentPlaceholderIndex.value]
})

// Start placeholder rotation when component mounts
function startPlaceholderRotation() {
  placeholderInterval = setInterval(() => {
    if (!chatStore.hasUnreadMessage) {
      currentPlaceholderIndex.value = (currentPlaceholderIndex.value + 1) % placeholderMessages.value.length
    }
  }, 6000)
}

// Stop placeholder rotation
function stopPlaceholderRotation() {
  if (placeholderInterval) {
    clearInterval(placeholderInterval)
    placeholderInterval = null
  }
}

// Watch for chat store open state
watch(() => chatStore.isOpen, (isOpen) => {
  if (isOpen) {
    startPlaceholderRotation()
    // Scroll to bottom when opened
    nextTick(() => scrollToBottom())
  } else {
    stopPlaceholderRotation()
  }
})

// Scroll chat to bottom when messages update
watch(() => chatStore.messages, () => {
  nextTick(scrollToBottom)
}, { deep: true })

function scrollToBottom() {
  const box = document.querySelector('.chat-messages')
  if (box) box.scrollTop = box.scrollHeight
}

function sendChat(event) {
  if (event) {
    event.preventDefault()
    event.stopPropagation()
  }

  if (!auth.userId) {
    ElMessage.warning('请先登录')
    return
  }
  const text = (chatInput.value || '').trim()
  if (!text) {
    ElMessage.warning('请输入内容')
    return
  }

  chatStore.sendMessage(text)
  chatInput.value = ''

  // Reset textarea height
  nextTick(() => {
    const textarea = document.querySelector('.chat-input-area')
    if (textarea) {
      textarea.style.height = 'auto'
    }
  })
}

function handleEnter(event) {
  // Only send on Enter without Shift
  event.preventDefault()
  event.stopPropagation()
  event.stopImmediatePropagation()

  sendChat()
  return false
}

// Auto-resize textarea
function handleInput(event) {
  const textarea = event.target
  textarea.style.height = 'auto'
  textarea.style.height = Math.min(textarea.scrollHeight, 160) + 'px'
}
</script>

<template>
  <!-- Chat Trigger Bar - Fixed position at top center -->
<!--  <div-->
<!--    v-if="!chatStore.isOpen"-->
<!--    class="chat-trigger-bar"-->
<!--    @click="chatStore.open"-->
<!--    :class="{ 'has-unread': chatStore.hasUnreadMessage }"-->
<!--  >-->
<!--    <el-icon class="trigger-icon"><ChatLineRound /></el-icon>-->
<!--    <div class="rolling-text-container">-->
<!--      <transition name="fade-slide" mode="out-in">-->
<!--        <span :key="chatStore.hasUnreadMessage ? 'unread' : currentPlaceholderIndex" class="rolling-text">-->
<!--          {{ displayText }}-->
<!--        </span>-->
<!--      </transition>-->
<!--    </div>-->
<!--  </div>-->

  <!-- Chat Modal -->
  <transition name="chat-expand">
    <div v-if="chatStore.isOpen" class="chat-wrapper">
      <div class="blur-overlay" @click="chatStore.close"></div>
      <div class="chat-modal" :class="{ 'is-fullscreen': chatStore.isFullScreen }">
        <div class="chat-inner">
          <!-- Header -->
          <div class="chat-header">
            <div class="chat-header-left">
              <el-icon class="ai-icon"><Cpu /></el-icon>
              <span class="chat-title">AI Assistant {{ chatStore.mode === 'analysis' ? '(Analysis Mode)' : '' }}</span>
            </div>
            <div class="chat-header-right">
              <el-tooltip content="清空对话" placement="bottom">
                <el-button circle text @click="chatStore.clearContext">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </el-tooltip>
              <el-button circle text @click="chatStore.toggleFullScreen">
                <el-icon><FullScreen /></el-icon>
              </el-button>
              <el-button circle text @click="chatStore.close">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </div>

          <!-- Messages -->
          <div class="chat-messages">
            <div v-if="chatStore.messages.length === 0" class="empty-state">
              <el-icon class="empty-icon"><ChatDotRound /></el-icon>
              <p>有什么我可以帮你的吗？</p>
            </div>

            <div
              v-for="m in chatStore.messages"
              :key="m.id"
              class="message-row"
              :class="m.role"
            >
              <div class="avatar-container">
                <el-avatar
                  v-if="m.role === 'user'"
                  :size="36"
                  :src="auth.username ? 'https://ui-avatars.com/api/?name=' + auth.username : ''"
                  class="user-avatar-icon"
                >
                  User
                </el-avatar>
                <div v-else class="ai-avatar">
                  <el-icon><Cpu /></el-icon>
                </div>
              </div>
              <div class="message-content">
                <!-- User message -->
                <div v-if="m.role === 'user'" class="bubble">
                  {{ m.content }}
                </div>

                <!-- AI message with markdown -->
                <div v-else class="bubble markdown-body">
                  <MarkdownRenderer :content="m.content || ''" />
                </div>
              </div>
            </div>

            <!-- Typing indicator -->
            <div v-if="chatStore.isStreaming" class="message-row assistant">
              <div class="avatar-container">
                <div class="ai-avatar">
                  <el-icon><Cpu /></el-icon>
                </div>
              </div>
              <div class="message-content">
                <div class="bubble typing-bubble">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer / Input -->
          <div class="chat-footer">
            <div class="input-wrapper">
              <div class="input-container" :class="{ 'has-content': chatInput && chatInput.trim().length > 0 }">
                <textarea
                  v-model="chatInput"
                  placeholder="输入你的问题... (按 Enter 发送，Shift+Enter 换行)"
                  @keydown.enter.prevent.exact="handleEnter"
                  @input="handleInput"
                  class="chat-input-area"
                  rows="1"
                  autocomplete="off"
                  autocorrect="off"
                  autocapitalize="off"
                  spellcheck="false"
                ></textarea>
                <button
                  class="send-btn"
                  @click="sendChat"
                  :disabled="!chatInput.trim()"
                  type="button"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="send-icon">
                    <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
              </div>
              <div class="input-hint">按 Enter 发送 · Shift+Enter 换行</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
/* Chat Trigger Bar - Fixed below header */
.chat-trigger-bar {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 100;
  width: 420px;
  height: 40px;
  border-radius: 20px;
  background: var(--el-fill-color);
  border: 1px solid var(--el-border-color);
  display: flex;
  align-items: center;
  padding: 0 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.5, 1);
  color: var(--el-text-color-secondary);
  box-shadow: 0 2px 6px rgba(0,0,0,0.04);
}

.chat-trigger-bar.has-unread {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.chat-trigger-bar:hover {
  background: var(--el-fill-color-light);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  transform: translateY(-1px);
}

.trigger-icon {
  font-size: 18px;
  margin-right: 10px;
  color: inherit;
}

.rolling-text-container {
  flex: 1;
  overflow: hidden;
  position: relative;
  height: 20px;
  display: flex;
  align-items: center;
}

.rolling-text {
  font-size: 14px;
  white-space: nowrap;
}

/* Transitions */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.4s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Chat Modal & Animations */
.chat-wrapper {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 80px;
}

.blur-overlay {
  position: fixed;
  inset: 0;
  backdrop-filter: blur(4px);
  background: rgba(0,0,0,0.15);
  z-index: 2000;
}

.chat-modal {
  position: relative;
  width: 800px;
  max-width: 90vw;
  height: 80vh;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
  z-index: 2001;
  display: flex;
  overflow: hidden;
  padding: 0;
  background: var(--el-bg-color);
  transition: background 0.3s;
  border: 1px solid var(--el-border-color-light);
}

.chat-modal.is-fullscreen {
  position: fixed;
  inset: 0;
  width: 100vw;
  height: 100vh;
  max-width: none;
  border-radius: 0;
  z-index: 2002;
}

.chat-inner {
  flex: 1;
  background: var(--el-bg-color);
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  z-index: 1;
  position: relative;
}

/* Parent transition container */
.chat-expand-enter-active,
.chat-expand-leave-active {
  transition-duration: 0.4s;
}

/* Child 1: Blur Overlay */
.chat-expand-enter-active .blur-overlay,
.chat-expand-leave-active .blur-overlay {
  transition: opacity 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.chat-expand-enter-from .blur-overlay,
.chat-expand-leave-to .blur-overlay {
  opacity: 0;
}

/* Child 2: Chat Modal */
.chat-expand-enter-active .chat-modal,
.chat-expand-leave-active .chat-modal {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.chat-expand-enter-from .chat-modal,
.chat-expand-leave-to .chat-modal {
  opacity: 0;
  transform: translateY(-30px) scale(0.96);
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
}

.chat-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-header-right {
  display: flex;
  align-items: center;
}

.ai-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

.chat-title {
  font-weight: 600;
  font-size: 16px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: var(--el-fill-color-extra-light);
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  gap: 16px;
  opacity: 0.6;
}

.empty-icon {
  font-size: 48px;
}

.message-row {
  display: flex;
  gap: 16px;
  max-width: 85%;
}

.message-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-row.assistant {
  align-self: flex-start;
}

.avatar-container {
  flex-shrink: 0;
  margin-top: 4px;
}

.ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #e0f2fe, #bae6fd);
  color: #0284c7;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.user-avatar-icon {
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
  font-weight: bold;
}

.bubble {
  padding: 10px 14px;
  border-radius: 16px;
  font-size: 15px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  width: fit-content;
  max-width: 100%;
  position: relative;
}

.message-row.user .bubble {
  background: var(--el-color-primary);
  color: white;
}

.message-row.user .bubble::after {
  content: '';
  position: absolute;
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-top: 6px solid transparent;
  border-bottom: 6px solid transparent;
  border-left: 6px solid var(--el-color-primary);
}

.message-row.assistant .bubble {
  background: var(--el-bg-color);
  border-top-left-radius: 2px;
  color: var(--el-text-color-primary);
}

.message-row.assistant .bubble::after {
  content: '';
  position: absolute;
  left: -5px;
  top: 50%;
  transform: translateY(-50%);
  width: 0;
  height: 0;
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
  border-right: 5px solid var(--el-bg-color);
}

/* Markdown styles within bubble */
.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
  font-size: 15px;
  line-height: 1.6;
  color: inherit;
  white-space: normal;
}

.typing-bubble {
  padding: 16px 20px;
  min-width: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}

/* Chat Footer - Modern Input Design */
.chat-footer {
  padding: 16px 20px 20px;
  background: linear-gradient(to top, var(--el-bg-color) 0%, var(--el-bg-color) 80%, transparent 100%);
}

.input-wrapper {
  max-width: 768px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  background: var(--el-fill-color-blank);
  padding: 12px 16px;
  border-radius: 24px;
  border: 1px solid var(--el-border-color-lighter);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 0 0 1px rgba(0, 0, 0, 0.02) inset;
}

.input-container:hover {
  border-color: var(--el-border-color);
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.06),
    0 0 0 1px rgba(0, 0, 0, 0.03) inset;
}

.input-container:focus-within {
  border-color: var(--el-color-primary);
  background: var(--el-bg-color);
  box-shadow:
    0 0 0 3px var(--el-color-primary-light-9),
    0 4px 16px rgba(0, 0, 0, 0.08);
}

.input-container.has-content {
  border-color: var(--el-color-primary-light-5);
}

.chat-input-area {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  padding: 10px 4px;
  font-size: 15px;
  line-height: 1.6;
  resize: none;
  overflow-y: auto;
  font-family: inherit;
  color: var(--el-text-color-primary);
  max-height: 160px;
  min-height: 28px;
}

.chat-input-area::placeholder {
  color: var(--el-text-color-placeholder);
  opacity: 0.8;
}

/* Modern Send Button */
.send-btn {
  background: var(--el-color-primary);
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  flex-shrink: 0;
  margin-bottom: 4px;
  box-shadow:
    0 2px 8px var(--el-color-primary-light-5),
    0 4px 12px var(--el-color-primary-light-8);
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.08) rotate(-5deg);
  background: var(--el-color-primary-light-3);
  box-shadow:
    0 4px 12px var(--el-color-primary-light-3),
    0 6px 20px var(--el-color-primary-light-7);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.send-btn:disabled {
  background: var(--el-fill-color-darker);
  box-shadow: none;
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.send-icon {
  width: 18px;
  height: 18px;
  color: white;
  transition: transform 0.25s ease;
}

.send-btn:hover:not(:disabled) .send-icon {
  transform: translate(1px, -1px);
}

.input-hint {
  text-align: center;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  opacity: 0.7;
  user-select: none;
  padding: 0 8px;
}

/* Responsive Design */
@media (max-width: 1024px) {
  .chat-trigger-bar {
    width: 320px;
  }
}

@media (max-width: 768px) {
  .chat-wrapper {
    padding-top: 0;
    align-items: flex-end;
  }

  .chat-modal {
    width: 100vw;
    max-width: 100vw;
    height: 70vh;
    border-radius: 16px 16px 0 0;
  }

  .chat-inner {
    border-radius: 16px 16px 0 0;
  }
}

@media (max-width: 480px) {
  .chat-trigger-bar {
    width: calc(100vw - 32px);
  }

  .chat-trigger-bar .rolling-text-container {
    max-width: calc(100% - 40px);
  }

  .chat-trigger-bar .rolling-text {
    font-size: 13px;
  }
}
</style>
