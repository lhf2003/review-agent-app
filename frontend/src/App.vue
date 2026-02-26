<script setup>
import { ref, watch, onMounted, nextTick, computed, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Moon, Sunny, FullScreen, Menu as MenuIcon, WarningFilled, Reading, Delete } from '@element-plus/icons-vue'
import { useAuthStore } from './stores/auth'
import { useChatStore } from './stores/chat'
import { useThemeStore } from './stores/theme'
import { api } from './api/http'
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

// ... (renderMarkdown function remains unchanged)
function renderMarkdown(content) {
  if (!content) return ''
  let processed = content

  // 0. 保护代码块
  const codeBlocks = []
  processed = processed.replace(/(```[\s\S]*?```|`[^`\n]+`)/g, (match) => {
    codeBlocks.push(match)
    return `\u0000CODE_BLOCK_${codeBlocks.length - 1}\u0000`
  })

  // 1. 保护表格 (关键步骤！)
  const tableBlocks = []
  processed = processed.replace(/((?:(?:^|\n)[ \t]*\|.*\|[ \t]*)+)/g, (match) => {
    tableBlocks.push(match)
    return `\u0000TABLE_BLOCK_${tableBlocks.length - 1}\u0000`
  })

  // 2. 修复列表断行 ("1.\n内容" -> "1. 内容")
  processed = processed.replace(/(^|\n)([ \t]*)((\d+\.)|[-*])([ \t]*)\n+([ \t]*)(?=\S)/g, '$1$2$3 ')

  // 3. 合并连续空行
  processed = processed.replace(/\n{3,}/g, '\n\n')

  // 4. 修复标题和引用空格
  processed = processed.replace(/(^|\n)(#{1,6})([^\s#])/g, '$1$2 $3')
  processed = processed.replace(/(^|\n)(\s*>)([^\s>])/g, '$1$2 $3')

  // 5. 优化换行 (保护段落换行，合并单换行)
  processed = processed.replace(/\n\s*\n/g, '\u0000PARAGRAPH_BREAK\u0000')
  const isCJK = (char) => /[\u4e00-\u9fa5\u3000-\u303f\uff00-\uffef]/.test(char)
  processed = processed.replace(/([^\n\u0000])\s*\n\s*([^\n\u0000])/g, (match, prev, next) => {
    if (isCJK(prev) && isCJK(next)) {
      return `${prev}${next}`
    }
    return `${prev} ${next}`
  })
  processed = processed.replace(/\u0000PARAGRAPH_BREAK\u0000/g, '\n\n')

  // 6. 还原表格 (关键步骤！)
  processed = processed.replace(/\u0000TABLE_BLOCK_(\d+)\u0000/g, (match, index) => {
    return tableBlocks[index]
  })

  // 7. 还原代码块
  processed = processed.replace(/\u0000CODE_BLOCK_(\d+)\u0000/g, (match, index) => {
    return codeBlocks[index]
  })

  return md.render(processed)
}

const auth = useAuthStore()
const chatStore = useChatStore()
const themeStore = useThemeStore()
const profileForm = ref({ avatar: '', newPassword: '', confirm: '' })
const chatInput = ref('')

// Responsive State
const isMobile = ref(false)
const drawerVisible = ref(false)

function checkMobile() {
  isMobile.value = window.innerWidth <= 768
}

// Chat trigger logic
const placeholderMessages = ref(['你需要我的帮助吗？', '发现一个新文件，需要我分析吗？', '输入关键字搜索分析结果...', '试试问我关于代码的问题'])
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

const route = useRoute()
const router = useRouter()
const isAuthPage = computed(() => {
  return ['/login', '/register'].includes(route.path)
})

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)

  api.getChatPlaceholders().then(list => {
    if (Array.isArray(list) && list.length > 0) {
      placeholderMessages.value = list
    }
  })

  placeholderInterval = setInterval(() => {
    if (!chatStore.hasUnreadMessage) {
      currentPlaceholderIndex.value = (currentPlaceholderIndex.value + 1) % placeholderMessages.value.length
    }
  }, 6000)
  themeStore.initTheme()
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  if (placeholderInterval) clearInterval(placeholderInterval)
})

function toggleTheme(val) {
  themeStore.toggleTheme(val)
}

function navigateTo(path) {
  router.push(path)
}

// Scroll chat to bottom when messages update
watch(() => chatStore.messages, () => {
  nextTick(() => {
    const box = document.querySelector('.chat-messages')
    if (box) box.scrollTop = box.scrollHeight
  })
}, { deep: true })

function sendChat(event) {
  if (event) {
    event.preventDefault()
    event.stopPropagation()
  }

  if (!auth.userId) { ElMessage.warning('请先登录'); return }
  const text = (chatInput.value || '').trim()
  if (!text) { ElMessage.warning('请输入内容'); return }

  chatStore.sendMessage(text)
  chatInput.value = ''

  // 重置 textarea 高度
  nextTick(() => {
    const textarea = document.querySelector('.chat-input-area')
    if (textarea) {
      textarea.style.height = 'auto'
    }
  })
}

function handleEnter(event) {
  // 只在单独按下 Enter 键时发送消息
  event.preventDefault()
  event.stopPropagation()
  event.stopImmediatePropagation()

  sendChat()
  return false
}

async function saveAvatar() {
  if (!auth.userId || !profileForm.value.avatar) { ElMessage.warning('缺少用户ID或头像URL'); return }
  try {
    await api.updateUserInfo({ id: Number(auth.userId), avatar: profileForm.value.avatar })
    ElMessage.success('头像已更新')
  } catch (e) { ElMessage.error(`更新失败: ${e.message}`) }
}

watch(() => auth.isAuthenticated, (val) => {
  if (!val) {
    avatarDialog.value = false
  }
})
</script>

<template>
  <div v-if="isAuthPage" style="height:100vh; width: 100%;">
    <router-view />
  </div>
  <el-container v-else style="height:100vh;">
    <!-- Desktop Sidebar - Left Vertical Dock -->
    <div class="vertical-dock-container" v-if="!isMobile">
      <div class="dock-wrapper">
        <div class="dock-item" @click="navigateTo('/profile')">
          <el-avatar class="dock-avatar" :size="48" :src="auth.username ? 'https://ui-avatars.com/api/?name=' + auth.username : ''" />
        </div>
        <router-link to="/data" class="dock-item">
          <div class="dock-icon" :class="{ 'is-active': $route.path === '/data' }">
            <el-icon><List /></el-icon>
            <span class="dock-text">数据</span>
          </div>
        </router-link>
        <router-link to="/collections" class="dock-item">
          <div class="dock-icon" :class="{ 'is-active': $route.path === '/collections' }">
            <el-icon><Files /></el-icon>
            <span class="dock-text">合集</span>
          </div>
        </router-link>
        <router-link to="/quiz-history" class="dock-item">
          <div class="dock-icon" :class="{ 'is-active': $route.path === '/quiz-history' }">
            <el-icon><Reading /></el-icon>
            <span class="dock-text">学习</span>
          </div>
        </router-link>
        <router-link to="/analysis" class="dock-item">
          <div class="dock-icon" :class="{ 'is-active': $route.path === '/analysis' }">
            <el-icon><Folder /></el-icon>
            <span class="dock-text">分析结果</span>
          </div>
        </router-link>
        <router-link to="/tags" class="dock-item">
          <div class="dock-icon" :class="{ 'is-active': $route.path === '/tags' }">
            <el-icon><PriceTag /></el-icon>
            <span class="dock-text">标签</span>
          </div>
        </router-link>
        <router-link to="/report" class="dock-item">
          <div class="dock-icon" :class="{ 'is-active': $route.path === '/report' }">
            <el-icon><Notebook /></el-icon>
            <span class="dock-text">报告</span>
          </div>
        </router-link>
        <div class="dock-spacer"></div>
        <router-link to="/config" class="dock-item">
          <div class="dock-icon" :class="{ 'is-active': $route.path === '/config' }">
            <el-icon><Setting /></el-icon>
            <span class="dock-text">配置</span>
          </div>
        </router-link>
      </div>
    </div>

    <!-- Mobile Drawer Sidebar -->
    <el-drawer v-model="drawerVisible" direction="ltr" size="240px" :with-header="false">
      <div class="mobile-menu-content">
        <div class="mobile-user-info" @click="navigateTo('/profile')">
           <el-avatar class="user-avatar" :size="50" :src="auth.username ? 'https://ui-avatars.com/api/?name=' + auth.username : ''" />
           <span style="margin-left: 12px; font-weight: 600;">{{ auth.username }}</span>
        </div>
        <el-menu router :default-active="$route.path" class="mobile-menu">
            <el-menu-item index="/data" @click="drawerVisible = false">
              <el-icon><List /></el-icon>
              <span>数据</span>
            </el-menu-item>
            <el-menu-item index="/collections" @click="drawerVisible = false">
              <el-icon><Files /></el-icon>
              <span>合集</span>
            </el-menu-item>
            <el-menu-item index="/quiz-history" @click="drawerVisible = false">
              <el-icon><Reading /></el-icon>
              <span>学习</span>
            </el-menu-item>
            <el-menu-item index="/analysis" @click="drawerVisible = false">
              <el-icon><Folder /></el-icon>
              <span>分析结果</span>
            </el-menu-item>
            <el-menu-item index="/tags" @click="drawerVisible = false">
              <el-icon><PriceTag /></el-icon>
              <span>标签</span>
            </el-menu-item>
            <el-menu-item index="/report" @click="drawerVisible = false">
              <el-icon><Notebook /></el-icon>
              <span>报告</span>
            </el-menu-item>
            <el-menu-item index="/config" @click="drawerVisible = false">
              <el-icon><Setting /></el-icon>
              <span>配置</span>
            </el-menu-item>
        </el-menu>
      </div>
    </el-drawer>

    <el-container>
      <el-header class="app-header">
        <div class="header-left">
          <el-button v-if="isMobile" @click="drawerVisible = true" link style="margin-right: 12px; font-size: 20px;">
            <el-icon><MenuIcon /></el-icon>
          </el-button>
        </div>
        <div class="header-center">
          <div class="chat-trigger-bar" @click="chatStore.open" :class="{ 'has-unread': chatStore.hasUnreadMessage }">
            <el-icon class="trigger-icon"><ChatLineRound /></el-icon>
            <div class="rolling-text-container" v-if="!isMobile">
              <transition name="fade-slide" mode="out-in">
                <span :key="chatStore.hasUnreadMessage ? 'unread' : currentPlaceholderIndex" class="rolling-text">{{ displayText }}</span>
              </transition>
            </div>
          </div>
        </div>
        <div class="header-right">
          <div class="theme-switch-wrapper" @click="toggleTheme(!themeStore.isDark)">
            <div class="theme-switch" :class="{ 'is-dark': themeStore.isDark }">
              <div class="switch-handle">
                <el-icon class="switch-icon">
                  <component :is="themeStore.isDark ? Moon : Sunny" />
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
    <div v-if="chatStore.isOpen" class="chat-wrapper">
      <div class="blur-overlay" @click="chatStore.close"></div>
      <div class="chat-modal" :class="{ 'is-fullscreen': chatStore.isFullScreen }">
        <div class="chat-inner">
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
          
          <div class="chat-messages">
            <div v-if="chatStore.messages.length === 0" class="empty-state">
              <el-icon class="empty-icon"><ChatDotRound /></el-icon>
              <p>有什么我可以帮你的吗？</p>
            </div>

            <div v-for="m in chatStore.messages" :key="m.id" class="message-row" :class="m.role">
              <div class="avatar-container">
                <el-avatar v-if="m.role === 'user'" :size="36" :src="auth.username ? 'https://ui-avatars.com/api/?name=' + auth.username : ''" class="user-avatar-icon">User</el-avatar>
                <div v-else class="ai-avatar">
                  <el-icon><Cpu /></el-icon>
                </div>
              </div>
              <div class="message-content">
                <!-- 用户消息内容 -->
                <div v-if="m.role === 'user'" class="bubble">
                  {{ m.content }}
                </div>

                <!-- AI 消息内容 -->
                <div v-else class="bubble markdown-body" v-html="renderMarkdown(m.content || '')"></div>
              </div>
            </div>

            <!-- 打字动画 -->
            <div v-if="chatStore.isStreaming" class="message-row assistant">
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
            <div class="input-wrapper">
              <div class="input-container" :class="{ 'has-content': chatInput && chatInput.trim().length > 0 }">
                <textarea
                  v-model="chatInput"
                  placeholder="输入你的问题... (按 Enter 发送，Shift+Enter 换行)"
                  @keydown.enter.prevent.exact="handleEnter"
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

  <el-dialog v-model="avatarDialog" width="380px" align-center>
    <div style="display:flex;flex-direction:column;align-items:center;gap:16px;">
      <el-avatar class="dialog-avatar" :size="128" :src="auth.username ? 'https://ui-avatars.com/api/?name=' + auth.username : ''" @click="toggleAvatarSize"/>
      <div style="width:100%;display:flex;justify-content:center;">
        <el-button type="danger" @click="onLogout">退出登录</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
/* Removed .app-aside and .app-header styles from here as they are moved to global styles */
.header-left { display:flex; align-items:center; min-width: 60px; }
.header-center { flex:1; display:flex; justify-content:center; }
.header-right { display:flex; align-items:center; justify-content:flex-end; min-width: 60px; }
.title { font-weight:bold; }
.app-main {
  padding: 24px 24px 24px 100px;
  max-width: 100%;
  margin: 0 auto;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  transition: padding 0.3s ease;
}

/* Vertical Left Dock Sidebar */
.vertical-dock-container {
  position: fixed;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 100;
  display: flex;
  justify-content: center;
  padding: 16px 8px;
  transition: all 0.3s ease;
}

.dock-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 12px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.3) inset,
    0 0 0 1px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.dock-item {
  position: relative;
  cursor: pointer;
  display: flex;
  align-items: center;
  text-decoration: none;
  width: 100%;
}

.dock-avatar {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.dock-avatar:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.18);
}

.dock-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: var(--el-text-color-primary);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: visible;
}

.dock-icon .el-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.dock-text {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateX(-10px) translateY(-50%);
  white-space: nowrap;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  opacity: 0;
  pointer-events: none;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: -1;
}

.dock-icon:hover {
  background: var(--el-fill-color-light);
  transform: translateX(10px);
}

.dock-icon:hover .dock-text {
  opacity: 1;
  transform: translateX(0) translateY(-50%);
}

.dock-icon.is-active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.dock-icon.is-active:hover {
  background: var(--el-color-primary-light-8);
  transform: translateX(10px);
}

.dock-icon.is-active .dock-text {
  color: var(--el-color-primary);
}

.dock-spacer {
  width: 40px;
  height: 1px;
  background: rgba(0, 0, 0, 0.1);
  margin: 8px 0;
  border-radius: 1px;
}

/* Dark mode adaptation */
html.dark .dock-wrapper {
  background: rgba(30, 30, 30, 0.75);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.24),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset,
    0 0 0 1px rgba(0, 0, 0, 0.1);
}

html.dark .dock-spacer {
  background: rgba(255, 255, 255, 0.1);
}
.dialog-avatar { cursor: zoom-in; transition: transform 200ms ease; }
.dialog-avatar:hover { transform: scale(1.03); }

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
.trigger-icon { font-size: 18px; margin-right: 10px; color: inherit; }
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
.chat-header-right { display: flex; align-items: center; }
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
/* 优化 markdown-body 在聊天气泡中的显示 */
.markdown-body {
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
  font-size: 15px;
  line-height: 1.6;
  color: inherit; /* 继承气泡文字颜色 */
  white-space: normal;
}

/* 减少段落间距，使其更紧凑 */
.markdown-body :deep(p) { 
  margin-top: 0;
  margin-bottom: 0.6em; 
}
.markdown-body :deep(p:last-child) { 
  margin-bottom: 0; 
}

/* 优化代码块样式 */
.markdown-body :deep(pre) { 
  margin: 0.5em 0;
  padding: 10px;
  border-radius: 6px;
  background-color: #282c34;
  overflow-x: auto;
}
.markdown-body :deep(code) {
  font-family: 'Fira Code', Consolas, monospace;
  background-color: rgba(175, 184, 193, 0.2);
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-size: 0.9em;
}

/* 优化列表缩进和间距 */
.markdown-body :deep(ul), 
.markdown-body :deep(ol) { 
  padding-left: 1.2em; 
  margin-bottom: 0.6em; 
  margin-top: 0;
}

/* 修复列表项内的段落间距，避免列表看起来断裂 */
.markdown-body :deep(li) {
  margin-bottom: 0.2em;
}
.markdown-body :deep(li > p) {
  margin: 0;
  display: inline; /* 强制列表项内容紧跟标记 */
}

/* 标题样式微调 */
.markdown-body :deep(h1), 
.markdown-body :deep(h2), 
.markdown-body :deep(h3) { 
  margin-top: 1em; 
  margin-bottom: 0.5em; 
  font-weight: 600; 
  line-height: 1.3; 
  font-size: 1.1em;
}

/* 表格样式美化 */
.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
  display: block; /* 允许横向滚动 */
  overflow-x: auto;
}

.markdown-body :deep(thead) {
  background-color: var(--el-fill-color-light); /* 适配浅色/深色模式 */
}

.markdown-body :deep(tr) {
  border-top: 1px solid var(--el-border-color);
  background-color: transparent;
}

/* 隔行变色 */
.markdown-body :deep(tr:nth-child(2n)) {
  background-color: var(--el-fill-color-lighter);
}

.markdown-body :deep(th),
.markdown-body :deep(td) {
  padding: 8px 12px;
  border: 1px solid var(--el-border-color);
  font-size: 14px;
}

.markdown-body :deep(th) {
  font-weight: 600;
  white-space: nowrap;
}

/* 修复表格内的代码块样式 */
.markdown-body :deep(table code) {
  white-space: pre-wrap; /* 允许表格内的代码换行 */
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

.typing-bubble { padding: 16px 20px; min-width: 60px; display: flex; align-items: center; justify-content: center; gap: 4px; }
.dot { width: 6px; height: 6px; border-radius: 50%; background: #94a3b8; animation: bounce 1.4s infinite ease-in-out both; }
.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }

/* 消息状态指示器 */
.message-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  font-size: 13px;
  animation: fadeIn 0.3s ease;
}

.message-status.sending {
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
}

.message-status.failed {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
  border: 1px solid var(--el-color-danger-light-7);
}

.message-status .el-icon {
  font-size: 16px;
}

.message-status .el-button {
  margin-left: auto;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 取消按钮 */
.message-row.user .bubble {
  position: relative;
}

.cancel-btn {
  position: absolute;
  right: -32px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  cursor: pointer;
  color: var(--el-text-color-secondary);
  transition: all 0.2s ease;
}

.cancel-btn:hover {
  color: var(--el-color-danger);
  transform: translateY(-50%) scale(1.1);
}

/* 消息状态样式 */
.message-row.sending {
  opacity: 0.7;
}

.message-row.failed {
  opacity: 0.8;
}

.message-row.streaming .bubble {
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
  }
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
@media (max-width: 1600px) {
  .app-main {
    padding: 24px 24px 24px 100px;
    max-width: 100%;
  }
}

@media (max-width: 1400px) {
  .app-main {
    padding: 24px 24px 24px 90px;
  }
}

@media (max-width: 1200px) {
  .app-main {
    padding: 20px 20px 20px 80px;
  }

  .vertical-dock-container {
    left: 8px;
  }

  .dock-wrapper {
    padding: 14px 10px;
    gap: 6px;
  }

  .dock-icon,
  .dock-avatar {
    width: 44px;
    height: 44px;
  }

  .dock-icon .el-icon {
    font-size: 22px;
  }

  .dock-text {
    font-size: 13px;
  }
}

@media (max-width: 1024px) {
  .app-main {
    padding: 20px 20px 20px 70px;
  }

  .vertical-dock-container {
    left: 6px;
  }

  .dock-wrapper {
    padding: 12px 8px;
    border-radius: 20px;
  }

  .dock-icon,
  .dock-avatar {
    width: 40px;
    height: 40px;
    border-radius: 10px;
  }

  .dock-icon .el-icon {
    font-size: 20px;
  }

  .dock-text {
    font-size: 13px;
  }

  .chat-trigger-bar {
    width: 320px;
  }
}

@media (max-width: 768px) {
  .app-main {
    padding: 16px 16px 16px 16px;
  }
}

/* Small screens adjustment */
@media (max-width: 480px) {
  .app-main {
    padding: 12px 12px 12px 12px;
  }

  .chat-trigger-bar {
    width: calc(100vw - 32px);
  }

  .chat-trigger-bar .rolling-text-container {
    max-width: calc(100% - 40px);
  }

  .chat-trigger-bar .rolling-text {
    font-size: 13px;
  }

  .theme-switch {
    width: 60px;
    height: 30px;
  }

  .switch-handle {
    width: 24px;
    height: 24px;
    left: 3px;
  }

  .theme-switch.is-dark .switch-handle {
    transform: translateX(30px);
  }

  .switch-icon {
    font-size: 16px;
  }
}

/* Large screens optimization */
@media (min-width: 1920px) {
  .app-main {
    padding: 32px 32px 32px 120px;
    max-width: 1600px;
  }
}

</style>
