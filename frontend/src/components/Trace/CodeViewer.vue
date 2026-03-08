<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { UserFilled, Service, Clock, ArrowLeft } from '@element-plus/icons-vue'
import MarkdownRenderer from '../MarkdownRenderer.vue'

const router = useRouter()

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  fileName: {
    type: String,
    default: '对话内容'
  }
})

// 解析 JSONL 格式
const conversationData = computed(() => {
  if (!props.content?.trim()) return []

  const lines = props.content.trim().split('\n')
  const messages = []

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed || !trimmed.startsWith('{')) continue

    try {
      const obj = JSON.parse(trimmed)
      // 支持多种字段格式
      const request = obj.request || obj.user || obj.input || obj.prompt || obj.question || ''
      const reply = obj.reply || obj.assistant || obj.output || obj.response || obj.answer || obj.content || ''

      if (request || reply) {
        messages.push({
          request,
          reply,
          timestamp: obj.timestamp || obj.time || obj.date
        })
      }
    } catch {
      // 跳过无效行
    }
  }

  return messages
})

// 格式化时间
function formatTime(timestamp) {
  if (!timestamp) return ''
  try {
    const date = new Date(timestamp)
    return isNaN(date.getTime()) ? timestamp : date.toLocaleString('zh-CN')
  } catch {
    return timestamp
  }
}

function goBack() {
  router.push('/data')
}
</script>

<template>
  <div class="code-viewer-container">
    <div class="editor-header">
      <div class="header-left">
        <div class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </div>
        <div class="file-name">{{ fileName }}</div>
      </div>
      <div class="header-spacer"></div>
    </div>

    <!-- JSONL 对话视图 -->
    <div class="conversation-viewer custom-scrollbar">
      <div class="conversation-list">
        <div
          v-for="(msg, index) in conversationData"
          :key="index"
          class="conversation-item"
        >
          <!-- 时间戳 -->
          <div v-if="msg.timestamp" class="timestamp-bar">
            <el-icon><Clock /></el-icon>
            <span class="time-text">{{ formatTime(msg.timestamp) }}</span>
          </div>

          <!-- 用户请求 -->
          <div v-if="msg.request" class="message-group user">
            <div class="avatar-container">
              <el-avatar :size="32" :icon="UserFilled" class="user-avatar" />
            </div>
            <div class="bubble-container">
              <div class="message-bubble user-bubble">
                <div class="bubble-content">{{ msg.request }}</div>
              </div>
            </div>
          </div>

          <!-- AI 回复 -->
          <div v-if="msg.reply" class="message-group ai">
            <div class="avatar-container">
              <el-avatar :size="32" :icon="Service" class="ai-avatar" />
            </div>
            <div class="bubble-container">
              <div class="message-bubble ai-bubble">
                <div class="bubble-content">
                  <MarkdownRenderer :content="msg.reply" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="conversationData.length === 0" class="empty-state">
          <el-empty description="暂无对话内容" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.code-viewer-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color);
  position: relative;
  /* Specific Code Viewer Colors */
  --cv-bg-light: #ffffff;
  --cv-text-light: #24292e; /* GitHub Dark Gray */
  --cv-bg-dark: #0d1117;   /* GitHub Dark Bg */
  --cv-text-dark: #c9d1d9; /* GitHub Dark Text */
  
  --cv-highlight-bg-light: #fff8c5; /* Light Yellow */
  --cv-highlight-text-light: #24292e;
  
  --cv-highlight-bg-dark: rgba(187, 128, 9, 0.15); /* Dark Gold Low Opacity */
  --cv-highlight-text-dark: #e3b341; /* Gold Text */
  
  --cv-active-bg-light: #fffbdd;
  --cv-active-border-light: #d29922;
  
  --cv-active-bg-dark: rgba(187, 128, 9, 0.3);
  --cv-active-border-dark: #e3b341;
}

.editor-header {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background-color: var(--el-bg-color-overlay);
  border-bottom: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.back-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 6px;
  color: var(--el-text-color-secondary);
  transition: all 0.2s;
}

.back-btn:hover {
  background-color: var(--el-fill-color);
  color: var(--el-text-color-primary);
}

.file-name {
  font-family: 'JetBrains Mono', 'IBM Plex Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  font-size: 13px;
  color: var(--el-text-color-regular);
  font-weight: 500;
}

.header-spacer {
  width: 28px;
}

/* Custom Scrollbar */
.custom-scrollbar::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 5px;
  border: 2px solid transparent;
  background-clip: content-box;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(0, 0, 0, 0.4);
}

:global(html.dark) .custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.2);
}

:global(html.dark) .custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background-color: rgba(255, 255, 255, 0.4);
}

.custom-scrollbar::-webkit-scrollbar-corner {
  background: transparent;
}

/* JSONL Conversation Viewer Styles */
.conversation-viewer {
  flex: 1;
  overflow: auto;
  background-color: var(--el-fill-color-lighter);
  padding: 20px;
}

:global(html.dark) .conversation-viewer {
  background-color: var(--el-bg-color);
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.conversation-item {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-group {
  display: flex;
  gap: 12px;
  max-width: 85%;
}

.message-group.user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.message-group.ai {
  align-self: flex-start;
}

.avatar-container {
  flex-shrink: 0;
  margin-top: 4px;
}

.user-avatar {
  background-color: var(--el-color-primary);
  color: white;
}

.ai-avatar {
  background-color: var(--el-color-success);
  color: white;
}

.bubble-container {
  display: flex;
  flex-direction: column;
}

.message-group.user .bubble-container {
  align-items: flex-end;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: var(--el-box-shadow-light);
}

.user-bubble {
  background-color: var(--el-color-primary);
  color: #ffffff;
  border-bottom-right-radius: 4px;
}

.ai-bubble {
  background-color: var(--el-bg-color);
  color: var(--el-text-color-primary);
  border-bottom-left-radius: 4px;
  border: 1px solid var(--el-border-color-light);
}

.bubble-content {
  :deep(p) {
    margin: 0 0 8px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  :deep(pre) {
    background: rgba(0, 0, 0, 0.05);
    padding: 12px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 8px 0;
  }
}

/* Dark mode adjustments for conversation */
:global(html.dark) .user-bubble {
  background-color: var(--el-color-primary);
}

:global(html.dark) .ai-bubble {
  background-color: var(--el-bg-color-overlay);
  border-color: var(--el-border-color-darker);
}

:global(html.dark) .bubble-content :deep(pre) {
  background: rgba(255, 255, 255, 0.05);
}

/* 时间戳样式 */
.timestamp-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.timestamp-bar .time-text {
  font-weight: 500;
}

/* 空状态样式 */
.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}
</style>
