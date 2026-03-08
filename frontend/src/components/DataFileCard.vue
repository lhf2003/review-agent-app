<script setup>
import { computed } from 'vue'
import { Document, ChatDotRound, ChatLineRound, View, Delete, Loading, WarningFilled, RefreshRight, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

/**
 * DataFileCard - 文件数据卡片组件
 * Glassmorphism 风格，状态驱动的视觉设计
 * 
 * @author Review Agent
 * @version 1.0.0
 */
const props = defineProps({
  data: {
    type: Object,
    required: true,
    validator: (value) => {
      return value && typeof value.id !== 'undefined'
    }
  },
  compact: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['view-content', 'analyze', 'view-result', 'delete', 'retry'])

// 状态配置映射
const statusConfig = computed(() => {
  const configs = {
    0: { // 未分析
      label: '未分析',
      type: 'info',
      color: '#6B7280',
      bgColor: 'rgba(107, 114, 128, 0.1)',
      borderColor: 'rgba(107, 114, 128, 0.2)',
      icon: Document,
      actionText: '开始分析',
      actionType: 'primary',
      pulse: false
    },
    1: { // 正在分析
      label: '分析中',
      type: 'primary',
      color: '#3B82F6',
      bgColor: 'rgba(59, 130, 246, 0.1)',
      borderColor: 'rgba(59, 130, 246, 0.3)',
      icon: Loading,
      actionText: '分析中...',
      actionType: 'warning',
      pulse: true
    },
    2: { // 已分析
      label: '已分析',
      type: 'success',
      color: '#10B981',
      bgColor: 'rgba(16, 185, 129, 0.1)',
      borderColor: 'rgba(16, 185, 129, 0.3)',
      icon: Check,
      actionText: '查看结果',
      actionType: 'success',
      pulse: false
    },
    3: { // 有更新
      label: '有更新',
      type: 'warning',
      color: '#F59E0B',
      bgColor: 'rgba(245, 158, 11, 0.1)',
      borderColor: 'rgba(245, 158, 11, 0.3)',
      icon: RefreshRight,
      actionText: '重新分析',
      actionType: 'warning',
      pulse: false
    },
    4: { // 失败
      label: '失败',
      type: 'danger',
      color: '#EF4444',
      bgColor: 'rgba(239, 68, 68, 0.1)',
      borderColor: 'rgba(239, 68, 68, 0.3)',
      icon: WarningFilled,
      actionText: '重试',
      actionType: 'danger',
      pulse: false
    }
  }
  return configs[props.data.processedStatus] || configs[0]
})

// 来源图标映射
const sourceIcon = computed(() => {
  const icons = {
    0: { icon: Document, label: '本地文件', color: '#6B7280' },
    1: { icon: ChatDotRound, label: 'Gemini', color: '#8B5CF6' },
    2: { icon: ChatLineRound, label: 'ChatGPT', color: '#10A37F' }
  }
  return icons[props.data.source] || icons[0]
})

// 卡片类名
const cardClasses = computed(() => ({
  'compact-mode': props.compact,
  [`status-${props.data.processedStatus}`]: true,
  'is-pulse': statusConfig.value.pulse
}))

// 卡片样式（动态边框色）
const cardStyle = computed(() => ({
  '--status-color': statusConfig.value.color,
  '--status-bg': statusConfig.value.bgColor,
  '--status-border': statusConfig.value.borderColor
}))

// 格式化日期
function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 格式化文件名（截断）
function formatFileName(name) {
  if (!name) return `文件 #${props.data.id}`
  if (name.length <= 30) return name
  const ext = name.split('.').pop()
  const base = name.substring(0, name.length - ext.length - 1)
  if (base.length > 20) {
    return `${base.substring(0, 15)}...${base.slice(-5)}.${ext}`
  }
  return name
}

// 处理查看内容
function handleViewContent() {
  emit('view-content', props.data)
}

// 处理分析/查看结果
function handleAction() {
  if (props.data.processedStatus === 2) {
    emit('view-result', props.data)
  } else if (props.data.processedStatus === 4) {
    emit('retry', props.data)
  } else if (props.data.processedStatus !== 1) {
    emit('analyze', props.data)
  }
}

// 处理删除
async function handleDelete(event) {
  event.stopPropagation()
  
  try {
    await ElMessageBox.confirm(
      `确定要删除 "${props.data.fileName || `文件 #${props.data.id}`}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    emit('delete', props.data)
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <div
    class="data-file-card glass-panel"
    :class="cardClasses"
    :style="cardStyle"
  >
    <!-- 状态指示条 -->
    <div class="status-bar" :class="{ 'status-pulse': statusConfig.pulse }"></div>
    
    <!-- 卡片头部 -->
    <div class="card-header">
      <div class="file-icon-wrapper" :class="`source-${data.source}`">
        <el-icon :size="20">
          <component :is="sourceIcon.icon" />
        </el-icon>
      </div>
      
      <div class="status-badge" :style="{ 
        backgroundColor: statusConfig.bgColor,
        color: statusConfig.color,
        borderColor: statusConfig.borderColor
      }">
        <el-icon v-if="statusConfig.pulse" class="rotating">
          <component :is="statusConfig.icon" />
        </el-icon>
        <el-icon v-else :size="12">
          <component :is="statusConfig.icon" />
        </el-icon>
        <span>{{ statusConfig.label }}</span>
      </div>
    </div>

    <!-- 卡片主体 -->
    <div class="card-body">
      <h3 class="file-name" :title="data.fileName">
        {{ formatFileName(data.fileName) }}
      </h3>
      
      <div class="meta-info">
        <div class="meta-item time-item">
          <span>{{ formatDate(data.createdTime) }}</span>
        </div>
      </div>
      
      <!-- 来源标签（仅紧凑模式显示） -->
      <div v-if="compact" class="source-label">
        {{ sourceIcon.label }}
      </div>
    </div>

    <!-- 卡片底部操作栏 -->
    <div class="card-footer">
      <button 
        class="action-btn view-btn" 
        @click.stop="handleViewContent"
        title="查看文件内容"
      >
        <el-icon><View /></el-icon>
        <span>内容</span>
      </button>
      
      <button 
        class="action-btn primary-action"
        :class="[`action-${data.processedStatus}`]"
        :disabled="data.processedStatus === 1"
        @click.stop="handleAction"
      >
        <el-icon v-if="data.processedStatus === 1" class="rotating">
          <Loading />
        </el-icon>
        <span v-else>{{ statusConfig.actionText }}</span>
      </button>
      
      <button 
        class="action-btn delete-btn" 
        @click.stop="handleDelete"
        title="删除文件"
      >
        <el-icon><Delete /></el-icon>
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
// Glassmorphism 基础样式
.data-file-card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  overflow: hidden;
  
  // 玻璃态效果
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  
  // 状态指示条
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: var(--status-color);
    opacity: 0.6;
    transition: opacity 0.3s, height 0.3s;
  }
  
  &:hover {
    transform: translateY(-3px) scale(1.01);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.1);
    background: rgba(255, 255, 255, 0.85);
    border-color: rgba(255, 255, 255, 0.6);
    
    &::before {
      opacity: 1;
      height: 4px;
    }
    
    .card-footer {
      opacity: 1;
    }
  }
  
  &:active {
    transform: translateY(-1px) scale(0.99);
  }
}

// 状态脉冲动画
.status-pulse {
  animation: statusPulse 2s ease-in-out infinite;
}

@keyframes statusPulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

// 旋转动画
.rotating {
  animation: rotate 1.5s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

// 卡片头部
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

// 文件图标
.file-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-regular);
  transition: all 0.2s;
  
  &.source-0 { // 本地
    background: rgba(107, 114, 128, 0.1);
    color: #6B7280;
  }
  
  &.source-1 { // Gemini
    background: rgba(139, 92, 246, 0.1);
    color: #8B5CF6;
  }
  
  &.source-2 { // ChatGPT
    background: rgba(16, 163, 127, 0.1);
    color: #10A37F;
  }
}

// 状态徽章
.status-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid;
  line-height: 1;
}

// 卡片主体
.card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.4;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  
  .el-icon {
    font-size: 14px;
  }
}

.source-label {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  margin-top: 4px;
}

// 卡片底部操作栏
.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  opacity: 0.9;
  transition: opacity 0.2s;
}

.action-btn {
  border: none;
  background: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  font-family: inherit;
  border-radius: 8px;
  
  &.view-btn {
    padding: 8px 12px;
    gap: 4px;
    font-size: 13px;
    color: var(--el-text-color-regular);
    background: var(--el-fill-color-light);
    
    &:hover {
      background: var(--el-fill-color);
      color: var(--el-color-primary);
    }
  }
  
  &.primary-action {
    flex: 1;
    height: 36px;
    padding: 8px 16px;
    gap: 6px;
    font-size: 13px;
    font-weight: 500;
    border-radius: 18px;
    color: white;
    
    &:not(:disabled) {
      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }
      
      &:active {
        transform: translateY(0);
      }
    }
    
    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    
    // 不同状态的按钮颜色
    &.action-0 { // 未分析
      background: linear-gradient(135deg, #3B82F6, #2563EB);
    }
    
    &.action-2 { // 已分析
      background: linear-gradient(135deg, #10B981, #059669);
    }
    
    &.action-3 { // 有更新
      background: linear-gradient(135deg, #F59E0B, #D97706);
    }
    
    &.action-4 { // 失败
      background: linear-gradient(135deg, #EF4444, #DC2626);
    }
  }
  
  &.delete-btn {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--el-fill-color-light);
    color: var(--el-text-color-regular);
    
    &:hover {
      background: rgba(239, 68, 68, 0.1);
      color: #EF4444;
    }
  }
}

// 紧凑模式
.compact-mode {
  padding: 12px;
  
  .file-icon-wrapper {
    width: 32px;
    height: 32px;
  }
  
  .file-name {
    font-size: 14px;
    -webkit-line-clamp: 1;
  }
  
  .card-footer {
    margin-top: 12px;
    padding-top: 10px;
  }
  
  .action-btn.primary-action {
    height: 32px;
    font-size: 12px;
  }
}

// ==================== 暗黑模式适配 ====================
html.dark .data-file-card {
  background: rgba(30, 30, 35, 0.75);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  
  &::before {
    opacity: 0.8;
  }
  
  &:hover {
    background: rgba(35, 35, 40, 0.85);
    border-color: rgba(255, 255, 255, 0.15);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.5);
    
    &::before {
      opacity: 1;
    }
  }
  
  .file-name {
    color: rgba(255, 255, 255, 0.95);
  }
  
  .meta-item {
    color: rgba(255, 255, 255, 0.65);
  }
  
  .source-label {
    color: rgba(255, 255, 255, 0.5);
  }
  
  .card-footer {
    border-top-color: rgba(255, 255, 255, 0.08);
  }
  
  .action-btn {
    &.view-btn {
      background: rgba(255, 255, 255, 0.1);
      color: rgba(255, 255, 255, 0.8);
      
      &:hover {
        background: rgba(255, 255, 255, 0.15);
        color: white;
      }
    }
    
    &.delete-btn {
      background: rgba(255, 255, 255, 0.1);
      color: rgba(255, 255, 255, 0.8);
      
      &:hover {
        background: rgba(239, 68, 68, 0.2);
        color: #EF4444;
      }
    }
  }
}

// 响应式优化
@media (max-width: 640px) {
  .data-file-card {
    padding: 14px;
  }
  
  .card-footer {
    opacity: 1;
  }
  
  .action-btn {
    &.view-btn span,
    &.primary-action span {
      display: none;
    }
    
    &.primary-action {
      flex: unset;
      width: 40px;
    }
  }
}

// 减少动画偏好
@media (prefers-reduced-motion: reduce) {
  .data-file-card,
  .action-btn {
    transition: none;
  }
  
  .rotating {
    animation: none;
  }
  
  .status-pulse {
    animation: none;
  }
}
</style>