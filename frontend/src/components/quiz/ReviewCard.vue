<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Clock, WarningFilled, TrendCharts, Right, PriceTag, Close } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { quizApi } from '../../api/quiz.js'

/**
 * 复习推荐卡片组件
 * 基于遗忘曲线的智能复习推荐
 * UI Style: Apple Glassmorphism
 */
const props = defineProps({
  // 推荐数据
  recommendation: {
    type: Object,
    required: true,
    validator: (value) => {
      return value &&
             typeof value.mistakeId !== 'undefined' &&
             typeof value.questionId !== 'undefined'
    }
  },
  // 紧凑模式
  compact: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['start-review', 'dismiss'])

const router = useRouter()

// 计算遗忘天数(修复版:避免整数除法精度丢失)
const daysUntilForget = computed(() => {
  if (!props.recommendation.nextReviewDate) return 0
  const now = new Date()
  const reviewDate = new Date(props.recommendation.nextReviewDate)
  const diff = reviewDate - now

  // 转换为小时再判断,避免整数除法精度丢失
  const hours = diff / (1000 * 60 * 60)

  if (hours < 0) {
    // 逾期:返回负的天数(保留一位小数)
    return parseFloat((hours / 24).toFixed(1))
  }

  // 未逾期:向上取整天数(保守估计)
  return Math.ceil(hours / 24)
})

// 计算紧急程度
const urgencyLevel = computed(() => {
  if (!props.recommendation.nextReviewDate) return 'normal'

  const days = daysUntilForget.value

  if (days < 0) return 'critical' // 已逾期
  if (days <= 1) return 'urgent'   // 1天内
  if (days <= 3) return 'high'     // 3天内
  return 'normal'                  // 正常
})

// 获取紧急程度配置
const urgencyConfig = computed(() => {
  const configs = {
    critical: {
      label: '已逾期',
      color: '#ff3b30', // Apple Red
      bgColor: 'rgba(255, 59, 48, 0.1)',
      icon: WarningFilled
    },
    urgent: {
      label: '紧急',
      color: '#ff9500', // Apple Orange
      bgColor: 'rgba(255, 149, 0, 0.1)',
      icon: Clock
    },
    high: {
      label: '建议复习',
      color: '#007aff', // Apple Blue
      bgColor: 'rgba(0, 122, 255, 0.1)',
      icon: TrendCharts
    },
    normal: {
      label: '计划中',
      color: '#34c759', // Apple Green
      bgColor: 'rgba(52, 199, 89, 0.1)',
      icon: Clock
    }
  }
  return configs[urgencyLevel.value]
})

// 计算卡片类名
const cardClasses = computed(() => {
  return {
    'compact-mode': props.compact,
    [`urgency-${urgencyLevel.value}`]: true
  }
})

// 获取推荐原因(优化版:精确显示逾期时间)
const recommendationReason = computed(() => {
  const days = daysUntilForget.value
  const mistakeCount = props.recommendation.mistakeCount || 1

  if (days < 0) {
    // 逾期情况
    const absDays = Math.abs(days)
    if (absDays < 1) {
      const hours = Math.round(absDays * 24)
      return hours === 0 ? '刚刚逾期' : `逾期 ${hours} 小时 · 错误 ${mistakeCount} 次`
    }
    return `逾期 ${Math.floor(absDays)} 天 · 错误 ${mistakeCount} 次`
  } else if (days === 0) {
    return '今天到期 · 建议立即复习'
  } else if (days === 1) {
    return '明天到期 · 请安排复习'
  } else {
    return `${days} 天后遗忘 · 错误 ${mistakeCount} 次`
  }
})

// 开始复习
function handleStartReview() {
  emit('start-review', props.recommendation)
}

// 忽略推荐（稍后复习）
async function handleDismiss(event) {
  event.stopPropagation()

  try {
    await quizApi.snoozeReview(props.recommendation.mistakeId, 24)
    emit('dismiss', props.recommendation)
  } catch {
    ElMessage.error('操作失败,请稍后重试')
  }
}

// 格式化日期
function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const now = new Date()
  const diff = date - now
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '明天'
  if (days === -1) return '昨天'
  if (days < -1) return `${Math.abs(days)} 天前`

  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 格式化题型
function formatQuestionType(type) {
  const typeMap = {
    'single_choice': '单选题',
    'multiple_choice': '多选题',
    'true_false': '判断题',
    'fill_blank': '填空题',
    'code_snippet': '代码题'
  }
  return typeMap[type] || type
}
</script>

<template>
  <div
    class="review-card glass-panel"
    :class="cardClasses"
    @click="handleStartReview"
  >
    <!-- 顶部状态栏 -->
    <div class="card-header">
      <div class="header-left">
        <div class="status-badge" :style="{ backgroundColor: urgencyConfig.bgColor, color: urgencyConfig.color }">
          <el-icon :size="12"><component :is="urgencyConfig.icon" /></el-icon>
          <span class="status-label">{{ urgencyConfig.label }}</span>
        </div>
      </div>
      <div class="header-right">
        <span class="review-date">{{ formatDate(recommendation.nextReviewDate) }}</span>
      </div>
    </div>

    <!-- 主要内容 -->
    <div class="card-body">
      <h3 class="question-text">
        {{ recommendation.questionText || `题目 #${recommendation.questionId}` }}
      </h3>

      <!-- 题型标签 -->
      <div class="question-type-badge" v-if="recommendation.questionType">
        <el-tag size="small" effect="plain" type="info">
          {{ formatQuestionType(recommendation.questionType) }}
        </el-tag>
      </div>

      <div class="meta-info">
        <div class="meta-item" v-if="recommendation.knowledgePoint">
          <el-icon><PriceTag /></el-icon>
          <span>{{ recommendation.knowledgePoint }}</span>
        </div>
        <div class="meta-item" v-else>
          <el-icon><PriceTag /></el-icon>
          <span>未分类知识点</span>
        </div>
        <div class="meta-item reason-item" :style="{ color: urgencyConfig.color }">
          <el-icon><component :is="urgencyConfig.icon" /></el-icon>
          <span>{{ recommendationReason }}</span>
        </div>
      </div>

      <!-- 数据完整性提示 -->
      <div class="data-warning" v-if="!recommendation.questionText || !recommendation.knowledgePoint">
        <el-icon><WarningFilled /></el-icon>
        <span>题目数据不完整，建议重新生成题库</span>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="card-actions">
      <button class="action-btn start-btn" @click.stop="handleStartReview">
        <span>开始复习</span>
        <el-icon><Right /></el-icon>
      </button>
      <button
        v-if="!compact"
        class="action-btn dismiss-btn"
        @click.stop="handleDismiss"
        title="稍后复习（24小时后提醒）"
      >
        <el-icon><Close /></el-icon>
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.review-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-radius: 16px;
  cursor: pointer;

  // Glassmorphism - 与 QuizHistoryPage.vue 保持一致
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur)) saturate(180%);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(180%);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-sm);
  transition: box-shadow 0.25s ease, transform 0.25s ease, background-color 0.25s ease !important;

  &:hover {
    transform: scale(1.01);
    background: var(--glass-surface-hover);
    box-shadow: var(--shadow-md);
    z-index: 1;
  }

  &:active {
    transform: scale(0.98);
  }
}

// Header
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
}

.review-date {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

// Body
.card-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-text {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.question-type-badge {
  margin-top: 4px;
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
}

.reason-item {
  font-weight: 500;
}

.data-warning {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 12px;
  background: rgba(255, 149, 0, 0.08);
  border-radius: 8px;
  font-size: 12px;
  color: #ff9500;
  border: 1px solid rgba(255, 149, 0, 0.2);
}

// Actions
.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;

  // Mobile: always visible
  @media (max-width: 768px) {
    opacity: 1 !important;
    transform: none !important;
  }
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

  &.start-btn {
    flex: 1;
    background: var(--el-color-primary);
    color: white;
    height: 36px;
    border-radius: 18px;
    padding: 8px 16px;
    gap: 6px;
    font-size: 13px;
    font-weight: 500;

    &:hover {
      background: var(--el-color-primary-light-3);
    }

    &:active {
      background: var(--el-color-primary-dark-2);
    }
  }

  &.dismiss-btn {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--el-fill-color-light);
    color: var(--el-text-color-regular);

    &:hover {
      background: var(--el-fill-color);
      color: var(--el-color-danger);
    }
  }
}

// Compact Mode
.compact-mode {
  padding: 12px;
  gap: 8px;

  .question-text {
    font-size: 14px;
    -webkit-line-clamp: 1;
  }

  .card-actions {
    margin-top: 0;
  }
}

</style>
