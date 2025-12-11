<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useAuthStore } from './stores/auth'
import { api } from './api/http'
import md5 from 'blueimp-md5'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (__) {}
    }
    return '' // use external default escaping
  }
})
const auth = useAuthStore()
const isDark = ref(true)
const avatarDialog = ref(false)
const avatarSizeLarge = ref(false)
const profileForm = ref({ avatar: '', newPassword: '', confirm: '' })
const chatOpen = ref(false)
const chatInput = ref('')
const chatMessages = ref([])
const isStreaming = ref(false)
let chatStreamCtl = null

// Chat trigger logic
const placeholderMessages = ['你需要我的帮助吗？', '发现一个新文件，需要我分析吗？', '输入关键字搜索分析结果...', '试试问我关于代码的问题']
const currentPlaceholderIndex = ref(0)
let placeholderInterval = null

onMounted(() => {
  placeholderInterval = setInterval(() => {
    currentPlaceholderIndex.value = (currentPlaceholderIndex.value + 1) % placeholderMessages.length
  }, 4000)
})

function toggleTheme(val) {
  isDark.value = val
  if (val) {
    document.documentElement.classList.add('dark')
    localStorage.setItem('theme', 'dark')
  } else {
    document.documentElement.classList.remove('dark')
    localStorage.setItem('theme', 'light')
  }
}

onMounted(() => {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'light') {
    isDark.value = false
    document.documentElement.classList.remove('dark')
  } else {
    isDark.value = true
    document.documentElement.classList.add('dark')
    if (!savedTheme) localStorage.setItem('theme', 'dark')
  }
})

function openAvatar() { avatarDialog.value = true }
function toggleAvatarSize() { avatarSizeLarge.value = !avatarSizeLarge.value }
function openChat() { chatOpen.value = true }
function closeChat() {
  chatOpen.value = false
  if (chatStreamCtl && isStreaming.value) {
    chatStreamCtl.cancel()
    isStreaming.value = false
  }
}

function sendChat() {
  if (!auth.userId) { ElMessage.warning('请先登录'); return }
  const text = (chatInput.value || '').trim()
  if (!text) { ElMessage.warning('请输入内容'); return }
  chatMessages.value.push({ role: 'user', content: text })
  chatInput.value = ''
  isStreaming.value = true
  
  chatStreamCtl = api.chatStream(text, {
    onEvent: (chunk) => {
      const lastMsg = chatMessages.value[chatMessages.value.length - 1]
      if (!lastMsg || lastMsg.role !== 'assistant') {
        chatMessages.value.push({ role: 'assistant', content: '' })
      }
      chatMessages.value[chatMessages.value.length - 1].content += chunk
      const box = document.querySelector('.chat-messages')
      if (box) box.scrollTop = box.scrollHeight
    },
    onDone: () => { isStreaming.value = false },
    onError: (e) => { isStreaming.value = false; ElMessage.error('聊天失败: ' + (e?.message || e)) }
  })
}

async function saveAvatar() {
  if (!auth.userId || !profileForm.value.avatar) { ElMessage.warning('缺少用户ID或头像URL'); return }
  try {
    await api.updateUserInfo({ id: Number(auth.userId), avatar: profileForm.value.avatar })
    ElMessage.success('头像已更新')
  } catch (e) { ElMessage.error(`更新失败: ${e.message}`) }
}

function onLogout() {
  avatarDialog.value = false
  auth.logout()
}

watch(() => auth.isAuthenticated, (val) => {
  if (!val) {
    avatarDialog.value = false
  }
})
</script>

<template>
  <div v-if="['/login','/register'].includes($route.path)" style="height:100vh;">
    <router-view />
  </div>
  <el-container v-else style="height:100vh;">
    <el-aside width="65px" class="app-aside">
      <div class="aside-avatar">
        <el-tooltip content="个人中心" placement="right">
          <el-avatar class="user-avatar" :size="50" :src="auth.username ? 'https://ui-avatars.com/api/?name=' + auth.username : ''" @click="openAvatar"/>
        </el-tooltip>
      </div>
      <el-menu router :default-active="$route.path" class="aside-menu">
        <el-tooltip content="数据" placement="right">
          <template #default>
            <el-menu-item index="/data">
              <el-icon><List /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip>
        <el-tooltip content="分析结果" placement="right">
          <template #default>
            <el-menu-item index="/analysis">
              <el-icon><Folder /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip>
        <el-tooltip content="标签" placement="right">
          <template #default>
            <el-menu-item index="/tags">
              <el-icon><PriceTag /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip> 
        <el-tooltip content="同步记录" placement="right">
          <template #default>
            <el-menu-item index="/sync">
              <el-icon><Clock /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip>
        <el-tooltip content="词云" placement="right">
          <template #default>
            <el-menu-item index="/word-cloud">
              <el-icon><TrendCharts /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip>
        <div class="menu-spacer"></div>
        <el-tooltip content="配置" placement="right">
          <template #default>
            <el-menu-item index="/config">
              <el-icon><Setting /></el-icon>
            </el-menu-item>
          </template>
        </el-tooltip>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <div class="title">Review Agent</div>
        </div>
        <div class="header-center">
          <div class="chat-trigger-bar" @click="openChat">
            <el-icon class="trigger-icon"><ChatLineRound /></el-icon>
            <div class="rolling-text-container">
              <transition name="fade-slide" mode="out-in">
                <span :key="currentPlaceholderIndex" class="rolling-text">{{ placeholderMessages[currentPlaceholderIndex] }}</span>
              </transition>
            </div>
          </div>
        </div>
        <div class="header-right">
          <div class="theme-switch-wrapper" @click="toggleTheme(!isDark)">
            <div class="theme-switch" :class="{ 'is-dark': isDark }">
              <div class="switch-handle">
                <el-icon class="switch-icon">
                  <component :is="isDark ? Moon : Sunny" />
                </el-icon>
              </div>
            </div>
          </div>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <transition name="chat-expand">
    <div v-if="chatOpen" class="chat-wrapper">
      <div class="blur-overlay" @click="closeChat"></div>
      <div class="chat-modal">
        <div class="chat-inner">
          <div class="chat-header">
            <div class="chat-header-left">
              <el-icon class="ai-icon"><Cpu /></el-icon>
              <span class="chat-title">AI Assistant</span>
            </div>
            <el-button circle text @click="closeChat">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          
          <div class="chat-messages">
            <div v-if="chatMessages.length === 0" class="empty-state">
              <el-icon class="empty-icon"><ChatDotRound /></el-icon>
              <p>有什么我可以帮你的吗？</p>
            </div>
            
            <div v-for="(m, i) in chatMessages" :key="i" class="message-row" :class="m.role">
              <div class="avatar-container">
                <el-avatar v-if="m.role === 'user'" :size="36" :src="auth.username ? 'https://ui-avatars.com/api/?name=' + auth.username : ''" class="user-avatar-icon">User</el-avatar>
                <div v-else class="ai-avatar">
                  <el-icon><Cpu /></el-icon>
                </div>
              </div>
              <div class="message-content">
              <div class="bubble" v-if="m.role === 'user'">{{ m.content }}</div>
              <div class="bubble markdown-body" v-else v-html="md.render(m.content)"></div>
            </div>
            </div>
            
            <div v-if="isStreaming && chatMessages.length > 0 && chatMessages[chatMessages.length - 1].role !== 'assistant'" class="message-row assistant">
               <div class="avatar-container">
                 <div class="ai-avatar">
                   <el-icon><Cpu /></el-icon>
                 </div>
               </div>
               <div class="message-content">
                 <div class="bubble typing-bubble">
                   <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                 </div>
               </div>
            </div>
          </div>
          
          <div class="chat-footer">
            <div class="input-container">
              <el-input 
                v-model="chatInput" 
                placeholder="输入你的问题..." 
                @keyup.enter="sendChat" 
                type="textarea"
                :autosize="{ minRows: 1, maxRows: 4 }"
                resize="none"
                class="chat-input-area"
              />
              <el-button type="primary" circle class="send-btn" @click="sendChat" :disabled="!chatInput.trim() && !isStreaming">
                <el-icon><Position /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>

  <el-dialog v-model="avatarDialog" width="380px" align-center>
    <div style="display:flex;flex-direction:column;align-items:center;gap:16px;">
      <el-avatar class="dialog-avatar" :size="128" :src="auth.username ? 'https://ui-avatars.com/api/?name=' + auth.username : ''" @click="toggleAvatarSize"/>
      <div style="width:100%;">
        <el-form label-width="100px">
          <el-form-item label="用户名">
            <el-input :model-value="auth.username" disabled />
          </el-form-item>
          <el-form-item>
            <el-button type="danger" @click="onLogout">退出登录</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.app-aside { border-right: 1px solid var(--el-border-color); display:flex; flex-direction:column; background: var(--el-bg-color); }
.app-header { display:flex; align-items:center; justify-content:space-between; gap: 20px; }
.header-left { display:flex; align-items:center; min-width: 150px; }
.header-center { flex:1; display:flex; justify-content:center; }
.header-right { display:flex; align-items:center; justify-content:flex-end; min-width: 150px; }
.title { font-weight:bold; }
.app-main { padding:16px; }
.aside-avatar { display:flex; align-items:center; justify-content:center; padding:16px 8px; border-bottom: 1px solid var(--el-border-color); }
.aside-menu { flex:1; display:flex; flex-direction:column; }
.menu-spacer { flex:1; }
.user-avatar { cursor: pointer; transition: transform 200ms ease; }
.user-avatar:hover { transform: scale(1.05); }
.dialog-avatar { cursor: zoom-in; transition: transform 200ms ease; }
.dialog-avatar:hover { transform: scale(1.03); }
.aside-menu :deep(.el-menu-item) { justify-content: center; }

/* Chat Trigger Bar */
.chat-trigger-bar {
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
.chat-trigger-bar:hover {
  background: var(--el-fill-color-light);
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border-color: var(--el-color-primary-light-5);
  transform: translateY(-1px);
}
.trigger-icon { font-size: 18px; margin-right: 10px; color: var(--el-text-color-regular); }
.rolling-text-container { flex:1; overflow:hidden; position:relative; height: 20px; display:flex; align-items:center; }
.rolling-text { font-size: 14px; white-space:nowrap; }

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

/* Theme Switch Styles */
.theme-switch-wrapper {
  cursor: pointer;
  padding: 4px;
}

.theme-switch {
  width: 70px;
  height: 34px;
  background: linear-gradient(to right, #87CEEB, #4da9d5);
  border-radius: 17px;
  position: relative;
  transition: all 0.4s ease;
  box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
}

.theme-switch.is-dark {
  background: linear-gradient(to right, #2C3E50, #4b6cb7);
}

.switch-handle {
  width: 28px;
  height: 28px;
  background: #fff;
  border-radius: 50%;
  position: absolute;
  left: 3px;
  transition: all 0.4s cubic-bezier(0.68, -0.55, 0.27, 1.55);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.theme-switch.is-dark .switch-handle {
  transform: translateX(36px);
  background: #1a1a1a;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.5);
}

.switch-icon {
  font-size: 18px;
  color: #f1c40f;
  animation: rotate-icon 0.5s ease-out;
}

@keyframes rotate-icon {
  from { transform: rotate(-180deg) scale(0.5); opacity: 0; }
  to { transform: rotate(0) scale(1); opacity: 1; }
}

/* Chat Modal & Animations */
.chat-wrapper { position: fixed; inset:0; z-index:2000; display:flex; justify-content:center; align-items:flex-start; padding-top: 80px; }
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

@keyframes rotate-border {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Parent transition container - just manages timing */
.chat-expand-enter-active, .chat-expand-leave-active {
  transition-duration: 0.4s;
}

/* Child 1: Blur Overlay - Fades in/out */
.chat-expand-enter-active .blur-overlay,
.chat-expand-leave-active .blur-overlay {
  transition: opacity 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.chat-expand-enter-from .blur-overlay,
.chat-expand-leave-to .blur-overlay {
  opacity: 0;
}

/* Child 2: Chat Modal - Slides and Fades */
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
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
}
.chat-header-left { display: flex; align-items: center; gap: 10px; }
.ai-icon { font-size: 24px; color: var(--el-color-primary); }
.chat-title { font-weight: 600; font-size: 16px; }

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
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: var(--el-text-color-placeholder); gap: 16px; opacity: 0.6;
}
.empty-icon { font-size: 48px; }

.message-row { display: flex; gap: 16px; max-width: 85%; }
.message-row.user { align-self: flex-end; flex-direction: row-reverse; }
.message-row.assistant { align-self: flex-start; }

.avatar-container { flex-shrink: 0; margin-top: 4px; }
.ai-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: linear-gradient(135deg, #e0f2fe, #bae6fd);
  color: #0284c7;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px;
}
.user-avatar-icon { background: var(--el-color-primary-light-8); color: var(--el-color-primary); font-weight: bold; }

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
.markdown-body {
  font-family: -apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif;
  font-size: 15px;
  line-height: 1.6;
}
.markdown-body :deep(p) { margin-bottom: 10px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(pre) { 
  background-color: #282c34; 
  color: #abb2bf;
  border-radius: 8px; 
  padding: 16px; 
  overflow: auto;
  margin-bottom: 16px;
  font-family: 'Fira Code', Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace;
  box-shadow: inset 0 0 0 1px rgba(255,255,255,0.1);
}
.markdown-body :deep(code) {
  font-family: 'Fira Code', Consolas, Monaco, 'Andale Mono', 'Ubuntu Mono', monospace;
  background-color: rgba(175, 184, 193, 0.2);
  padding: 0.2em 0.4em;
  border-radius: 6px;
  font-size: 85%;
}
.markdown-body :deep(pre code) {
  background-color: transparent;
  padding: 0;
  font-size: 14px;
  color: inherit;
  font-family: inherit;
}
.markdown-body :deep(ul), .markdown-body :deep(ol) { padding-left: 20px; margin-bottom: 10px; }
.markdown-body :deep(h1), .markdown-body :deep(h2), .markdown-body :deep(h3) { margin-top: 16px; margin-bottom: 8px; font-weight: 600; line-height: 1.25; }

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

.typing-bubble { padding: 16px 20px; min-width: 60px; display: flex; align-items: center; justify-content: center; gap: 4px; }
.dot { width: 6px; height: 6px; border-radius: 50%; background: #94a3b8; animation: bounce 1.4s infinite ease-in-out both; }
.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }

.chat-footer {
  padding: 16px 24px;
  border-top: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
}
.chat-input-area :deep(.el-textarea__inner) {
  box-shadow: none !important;
  background: transparent !important;
  padding: 0;
  resize: none;
}

.input-container {
  display: flex; gap: 12px; align-items: flex-end;
  /* background: var(--el-fill-color-light); */
  padding: 10px 14px;
  border-radius: 24px;
  border: 1px solid var(--el-border-color);
  transition: all 0.2s;
  position: relative;
  z-index: 0;
  overflow: hidden;
}

.input-container:hover {
  border-color: transparent;
}

.input-container::before {
  content: '';
  position: absolute;
  top: 50%; left: 50%;
  width: 100vmax; height: 100vmax;
  transform: translate(-50%, -50%);
  background: conic-gradient(
    from 0deg,
    #ff0000, #ff7300, #fffb00, #48ff00, #00ffd5, #002bff, #7a00ff, #ff00c8, #ff0000
  );
  animation: rotate-border-centered 4s linear infinite;
  z-index: -2;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.input-container:hover::before {
  opacity: 1;
}

@keyframes rotate-border-centered {
  from { transform: translate(-50%, -50%) rotate(0deg); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

.input-container::after {
  content: '';
  position: absolute;
  inset: 2px;
  background: var(--el-bg-color);
  border-radius: 22px;
  z-index: -1;
}

.send-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: transform 0.2s, opacity 0.2s;
}
.send-btn:hover:not(:disabled) {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.4);
}
.send-btn:disabled {
  background: var(--el-fill-color-darker);
  opacity: 0.6;
  cursor: not-allowed;
}
.chat-input-area :deep(.el-textarea__inner) {
  box-shadow: none !important;
  background: transparent;
  padding: 8px 0;
  min-height: 24px !important;
  resize: none;
}
.send-btn { flex-shrink: 0; width: 40px; height: 40px; margin-bottom: 2px; }

</style>
