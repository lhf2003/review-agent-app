<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, WarningFilled, Document, EditPen, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const stats = ref({
  totalPending: 0,
  mistakeCount: 0,
  quizCount: 0,
  highPriorityCount: 0
})

const pendingReviews = ref([])

const hasPendingItems = computed(() => stats.value.totalPending > 0)

const props = defineProps({
  cardsVisible: {
    type: Boolean,
    default: false
  }
})

onMounted(async () => {
  await loadPendingStats()
  await loadPendingReviews()
})

async function loadPendingStats() {
  try {
    const resp = await request('/notification/pending/stats')
    if (resp) {
      stats.value = resp
    }
  } catch (e) {
    console.error('Failed to load pending stats:', e)
  }
}

async function loadPendingReviews() {
  try {
    loading.value = true
    const resp = await request('/notification/pending')
    pendingReviews.value = Array.isArray(resp) ? resp.slice(0, 5) : []
  } catch (e) {
    console.error('Failed to load pending reviews:', e)
  } finally {
    loading.value = false
  }
}

async function request(path) {
  const token = localStorage.getItem('token')
  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
  }

  const res = await fetch('/api' + path, { headers })

  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`)
  }

  return res.json()
}

function navigateToMistakeBook() {
  router.push('/mistake-book')
}

function navigateToQuizHistory() {
  router.push('/quiz-history')
}

function navigateToNotificationSettings() {
  router.push('/config/notification')
}

function getPriorityClass(priority) {
  if (priority >= 4) return 'high'
  if (priority >= 2) return 'medium'
  return 'low'
}

function getPriorityLabel(priority) {
  if (priority >= 4) return '高优先'
  if (priority >= 2) return '中等'
  return '一般'
}

function formatTime(dateTimeStr) {
  if (!dateTimeStr) return ''
  const date = new Date(dateTimeStr)
  const now = new Date()
  const diffMs = date - now
  const diffHours = Math.round(diffMs / (1000 * 60 * 60))

  if (diffHours < 0) {
    return '已过期'
  } else if (diffHours < 24) {
    return `${diffHours}小时后`
  } else {
    const diffDays = Math.round(diffHours / 24)
    return `${diffDays}天后`
  }
}
</script>

<template>
  <div class="pending-reminder" :class="{ visible: cardsVisible, 'has-items': hasPendingItems }">
    <div class="reminder-header">
      <div class="header-left">
        <el-icon class="bell-icon" :class="{ 'has-notification': hasPendingItems }">
          <Bell />
        </el-icon>
        <span class="header-title">复习提醒</span>
      </div>
      <el-button
        v-if="hasPendingItems"
        type="primary"
        text
        size="small"
        @click="navigateToNotificationSettings"
      >
        设置
      </el-button>
    </div>

    <div v-if="!hasPendingItems" class="no-pending">
      <el-icon class="check-icon"><WarningFilled /></el-icon>
      <span>暂无待复习内容</span>
    </div>

    <template v-else>
      <div class="stats-summary">
        <div class="stat-item" @click="navigateToMistakeBook">
          <span class="stat-value">{{ stats.mistakeCount }}</span>
          <span class="stat-label">错题待复习</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item" @click="navigateToQuizHistory">
          <span class="stat-value">{{ stats.quizCount }}</span>
          <span class="stat-label">测验待重练</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item high-priority">
          <span class="stat-value">{{ stats.highPriorityCount }}</span>
          <span class="stat-label">高优先</span>
        </div>
      </div>

      <div v-if="pendingReviews.length > 0" class="pending-list">
        <div class="list-header">
          <span>待复习项目</span>
          <el-button type="primary" text size="small" @click="navigateToMistakeBook">
            查看全部
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>

        <div
          v-for="item in pendingReviews"
          :key="`${item.type}-${item.referenceId}`"
          class="pending-item"
          :class="getPriorityClass(item.priority)"
        >
          <div class="item-icon">
            <el-icon v-if="item.type === 'MISTAKE'"><EditPen /></el-icon>
            <el-icon v-else><Document /></el-icon>
          </div>
          <div class="item-content">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-meta">
              <span v-if="item.knowledgePoint" class="knowledge-point">
                {{ item.knowledgePoint }}
              </span>
              <span class="review-time">{{ formatTime(item.nextReviewTime) }}</span>
            </div>
          </div>
          <div class="item-priority" :class="getPriorityClass(item.priority)">
            {{ getPriorityLabel(item.priority) }}
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.pending-reminder {
  background: rgba(255, 255, 255, 0.75);
  border-radius: 20px;
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.3) inset;
  border: none;
  padding: 20px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.pending-reminder.visible {
  animation: cardFadeIn 0.6s ease forwards;
}

@keyframes cardFadeIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.reminder-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.bell-icon {
  font-size: 22px;
  color: var(--el-text-color-secondary);
  transition: all 0.3s ease;
}

.bell-icon.has-notification {
  color: var(--el-color-warning);
  animation: bellRing 1s ease infinite;
}

@keyframes bellRing {
  0%, 100% { transform: rotate(0); }
  25% { transform: rotate(10deg); }
  75% { transform: rotate(-10deg); }
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.no-pending {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px;
  color: var(--el-text-color-secondary);
}

.check-icon {
  font-size: 32px;
  color: var(--el-color-success);
}

.stats-summary {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: 12px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.stat-item:hover {
  background: rgba(255, 255, 255, 0.5);
}

.stat-item.high-priority .stat-value {
  color: var(--el-color-danger);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-color-primary);
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--el-border-color-lighter);
}

.pending-list {
  margin-top: 16px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
}

.pending-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pending-item:hover {
  background: var(--el-fill-color-light);
  transform: translateX(4px);
}

.pending-item.high {
  border-left: 3px solid var(--el-color-danger);
}

.pending-item.medium {
  border-left: 3px solid var(--el-color-warning);
}

.pending-item.low {
  border-left: 3px solid var(--el-color-success);
}

.item-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--el-fill-color);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.knowledge-point {
  padding: 2px 6px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  border-radius: 4px;
}

.review-time {
  color: var(--el-color-warning);
}

.item-priority {
  font-size: 12px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 6px;
}

.item-priority.high {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}

.item-priority.medium {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}

.item-priority.low {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

/* Dark Mode */
html.dark .pending-reminder {
  background: rgba(30, 30, 30, 0.85);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.24),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset;
}

html.dark .stats-summary {
  background: rgba(50, 50, 50, 0.6);
}

html.dark .stat-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

html.dark .pending-item {
  background: rgba(50, 50, 50, 0.4);
}

html.dark .pending-item:hover {
  background: rgba(50, 50, 50, 0.6);
}
</style>
