<script setup>
import { computed } from 'vue'
import { UserFilled, Service, Clock } from '@element-plus/icons-vue'
import MarkdownRenderer from './MarkdownRenderer.vue'

/**
 * ConversationViewer - 对话内容展示组件
 * 解析 JSON Lines 格式并展示为聊天界面
 * 
 * 数据格式: JSON Lines (每行一个 JSON 对象)
 * { timestamp: string, request: string, reply: string }
 * 
 * @author Review Agent
 * @version 2.0.0
 */
const props = defineProps({
  content: {
    type: String,
    default: ''
  }
})

// 解析 JSON Lines 格式
const conversationData = computed(() => {
  if (!props.content?.trim()) return null
  
  const lines = props.content.trim().split('\n')
  const messages = []
  
  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed || !trimmed.startsWith('{')) continue
    
    try {
      const obj = JSON.parse(trimmed)
      if (obj.request || obj.reply) {
        messages.push({
          timestamp: obj.timestamp,
          request: obj.request || '',
          reply: obj.reply || ''
        })
      }
    } catch {
      // 跳过无效行
    }
  }
  
  return messages.length > 0 ? messages : null
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

// 格式化相对时间
function formatRelativeTime(timestamp) {
  if (!timestamp) return ''
  try {
    const date = new Date(timestamp)
    if (isNaN(date.getTime())) return ''
    
    const diff = Date.now() - date.getTime()
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)
    
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 30) return `${days}天前`
    return ''
  } catch {
    return ''
  }
}
</script>

<template>
  <div class="conversation-viewer">
    <div
      v-for="(msg, index) in conversationData"
      :key="index"
      class="conversation-item"
    >
      <!-- 时间戳 -->
      <div v-if="msg.timestamp" class="timestamp-bar">
        <el-icon><Clock /></el-icon>
        <span class="time-text">{{ formatTime(msg.timestamp) }}</span>
        <span v-if="formatRelativeTime(msg.timestamp)" class="relative-time">
          ({{ formatRelativeTime(msg.timestamp) }})
        </span>
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
          <div class="message-meta">用户请求</div>
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
          <div class="message-meta">AI 回复</div>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-if="!conversationData" class="empty-state">
      <el-empty description="暂无对话内容" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.conversation-viewer {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 16px;
  min-height: 200px;
}

.conversation-item {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.timestamp-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  
  .time-text {
    font-weight: 500;
  }
  
  .relative-time {
    opacity: 0.7;
  }
}

.message-group {
  display: flex;
  gap: 12px;
  max-width: 100%;
  
  &.user {
    flex-direction: row-reverse;
  }
}

.avatar-container {
  flex-shrink: 0;
  margin-top: 4px;
}

.user-avatar {
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  color: white;
}

.ai-avatar {
  background: linear-gradient(135deg, #10B981, #059669);
  color: white;
}

.bubble-container {
  display: flex;
  flex-direction: column;
  max-width: 75%;
  
  .message-group.user & {
    align-items: flex-end;
  }
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  
  &.user-bubble {
    background: linear-gradient(135deg, #3B82F6, #2563EB);
    color: #ffffff;
    border-bottom-right-radius: 4px;
  }
  
  &.ai-bubble {
    background: rgba(255, 255, 255, 0.8);
    color: var(--el-text-color-primary);
    border-bottom-left-radius: 4px;
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.5);
  }
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

.message-meta {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  margin-top: 6px;
  padding: 0 4px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

html.dark {
  .message-bubble.ai-bubble {
    background: rgba(30, 30, 35, 0.8);
    border-color: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.9);
  }
  
  .message-meta {
    color: rgba(255, 255, 255, 0.5);
  }
  
  .timestamp-bar {
    color: rgba(255, 255, 255, 0.6);
  }
  
  .bubble-content :deep(pre) {
    background: rgba(255, 255, 255, 0.05);
  }
}

@media (max-width: 768px) {
  .conversation-viewer {
    padding: 12px;
    gap: 20px;
  }
  
  .message-group {
    gap: 8px;
  }
  
  .bubble-container {
    max-width: 85%;
  }
  
  .message-bubble {
    padding: 10px 14px;
    font-size: 13px;
  }
}
</style>