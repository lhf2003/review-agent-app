<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Clock, WarningFilled, TrendCharts, Right } from '@element-plus/icons-vue'
import { ElTag } from 'element-plus'

/**
 * 复习推荐卡片组件
 * 基于遗忘曲线的智能复习推荐
 */
const props = defineProps({
  // 推荐数据
  recommendation: {
    type: Object,
    required: true,
    validator: (value) => {
      return value &&
             typeof value.mistakeId !== 'undefined' &&
             typeof value.questionId !== 'undefined' &&
             typeof value.priority !== 'undefined'
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

// 计算遗忘天数
const daysUntilForget = computed(() => {
  if (!props.recommendation.nextReviewDate) return 0
  const now = new Date()
  const reviewDate = new Date(props.recommendation.nextReviewDate)
  const diff = reviewDate - now
  return Math.floor(diff / (1000 * 60 * 60 * 24))
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
      color: '#f56c6c',
      bgColor: 'rgba(245, 108, 108, 0.1)',
      icon: WarningFilled
    },
    urgent: {
      label: '紧急',
      color: '#e6a23c',
      bgColor: 'rgba(230, 162, 60, 0.1)',
      icon: Clock
    },
    high: {
      label: '建议复习',
      color: '#409eff',
      bgColor: 'rgba(64, 158, 255, 0.1)',
      icon: TrendCharts
    },
    normal: {
      label: '计划中',
      color: '#67c23a',
      bgColor: 'rgba(103, 194, 58, 0.1)',
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

// 获取推荐原因
const recommendationReason = computed(() => {
  const days = daysUntilForget.value
  const mistakeCount = props.recommendation.mistakeCount || 1

  if (days < 0) {
    return `已逾期 ${Math.abs(days)} 天，错误 ${mistakeCount} 次`
  } else if (days === 0) {
    return '今天到期，建议立即复习'
  } else if (days === 1) {
    return '明天到期，请安排复习'
  } else {
    return `${days} 天后遗忘，错误 ${mistakeCount} 次`
  }
})

// 开始复习
function handleStartReview() {
  emit('start-review', props.recommendation)
}

// 忽略推荐
function handleDismiss(event) {
  event.stopPropagation()
  emit('dismiss', props.recommendation)
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
</script>

<template>
  <div
    class="review-card"
    :class="cardClasses"
    @click="handleStartReview"
  >
    <!-- 左侧：优先级指示器 -->
    <div class="priority-indicator" :style="{ backgroundColor: urgencyConfig.color }">
      <el-icon class="indicator-icon">
        <component :is="urgencyConfig.icon" />
      </el-icon>
    </div>

    <!-- 中间：内容区域 -->
    <div class="card-content">
      <!-- 顶部标签栏 -->
      <div class="content-header">
        <div class="header-left">
          <el-tag
            :color="urgencyConfig.color"
            effect="dark"
            size="small"
            class="urgency-tag"
          >
            {{ urgencyConfig.label }}
          </el-tag>
          <span class="priority-badge">
            优先级 {{ recommendation.priority }}
          </span>
        </div>
        <div class="header-right">
          <span class="review-date">{{ formatDate(recommendation.nextReviewDate) }}</span>
        </div>
      </div>

      <!-- 题目预览 -->
      <div class="question-preview">
        <div class="question-text">
          {{ recommendation.questionText || '题目内容加载中...' }}
        </div>
      </div>

      <!-- 推荐原因 -->
      <div class="recommendation-reason">
        <el-icon class="reason-icon">
          <component :is="urgencyConfig.icon" />
        </el-icon>
        <span class="reason-text">{{ recommendationReason }}</span>
      </div>

      <!-- 知识点标签 -->
      <div v-if="recommendation.knowledgePoint" class="knowledge-tags">
        <div class="knowledge-tag">
          <el-icon><PriceTag /></el-icon>
          <span>{{ recommendation.knowledgePoint }}</span>
        </div>
      </div>
    </div>

    <!-- 右侧：操作按钮 -->
    <div class="card-actions">
      <el-button
        type="primary"
        :icon="Right"
        @click.stop="handleStartReview"
        class="start-button"
      >
        开始复习
      </el-button>
      <el-button
        v-if="!compact"
        circle
        text
        size="small"
        @click="handleDismiss"
        class="dismiss-button"
      >
        <el-icon><Close /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.review-card {
  display: flex;
  align-items: stretch;
  gap: 16px;
  padding: 20px;
  background: var(--el-bg-color);
  border-radius: 16px;
  border: 2px solid var(--el-border-color-light);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: var(--el-fill-color-light);
    opacity: 0;
    transition: opacity 0.3s ease;
    pointer-events: none;
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    border-color: var(--el-color-primary-light-5);

    .start-button {
      transform: translateX(4px);
    }

    &::before {
      opacity: 0.5;
    }
  }

  // 紧急程度样式
  &.urgency-critical {
    border-color: var(--urgency-critical, #f56c6c);
    background: linear-gradient(135deg, rgba(245, 108, 108, 0.05) 0%, var(--el-bg-color) 100%);
  }

  &.urgency-urgent {
    border-color: var(--urgency-urgent, #e6a23c);
    background: linear-gradient(135deg, rgba(230, 162, 60, 0.05) 0%, var(--el-bg-color) 100%);
  }

  &.urgency-high {
    border-color: var(--urgency-high, #409eff);
  }

  &.compact-mode {
    padding: 14px 16px;
    gap: 12px;
  }
}

// 优先级指示器
.priority-indicator {
  flex-shrink: 0;
  width: 6px;
  border-radius: 3px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;

  .indicator-icon {
    color: white;
    font-size: 20px;
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  .review-card:hover & {
    width: 48px;

    .indicator-icon {
      opacity: 1;
    }
  }
}

// 内容区域
.card-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
  z-index: 1;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.urgency-tag {
  font-weight: 600;
  border: none;
}

.priority-badge {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  padding: 4px 10px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-date {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}

// 题目预览
.question-preview {
  min-height: 0;
}

.question-text {
  font-size: 15px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.compact-mode .question-text {
  -webkit-line-clamp: 1;
  font-size: 14px;
}

// 推荐原因
.recommendation-reason {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;
  border-left: 3px solid var(--urgency-color, var(--el-color-primary));

  .reason-icon {
    font-size: 18px;
    color: var(--urgency-color, var(--el-color-primary));
  }

  .reason-text {
    font-size: 13px;
    font-weight: 500;
    color: var(--el-text-color-regular);
  }
}

.review-card.urgency-critical .recommendation-reason {
  background: rgba(245, 108, 108, 0.1);
  border-left-color: #f56c6c;
}

.review-card.urgency-urgent .recommendation-reason {
  background: rgba(230, 162, 60, 0.1);
  border-left-color: #e6a23c;
}

// 知识点标签
.knowledge-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.knowledge-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: var(--el-color-primary-light-9);
  border-radius: 6px;
  font-size: 13px;
  color: var(--el-color-primary-dark-2);
  font-weight: 500;

  .el-icon {
    font-size: 16px;
  }
}

// 操作按钮
.card-actions {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  position: relative;
  z-index: 1;
}

.start-button {
  transition: transform 0.3s ease;
}

.dismiss-button {
  align-self: center;

  &:hover {
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
  }
}

.compact-mode .card-actions {
  flex-direction: row;

  .dismiss-button {
    align-self: center;
  }
}

// 深色模式适配
:global(.dark) .review-card {
  &.urgency-critical {
    background: linear-gradient(135deg, rgba(245, 108, 108, 0.15) 0%, rgba(40, 40, 40, 0.8) 100%);
  }

  &.urgency-urgent {
    background: linear-gradient(135deg, rgba(230, 162, 60, 0.15) 0%, rgba(40, 40, 40, 0.8) 100%);
  }

  .recommendation-reason {
    background: rgba(255, 255, 255, 0.05);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .review-card {
    flex-direction: column;
    gap: 12px;
    padding: 16px;

    &.compact-mode {
      flex-direction: row;
      padding: 12px;
    }
  }

  .priority-indicator {
    width: 4px;

    .indicator-icon {
      display: none;
    }
  }

  .card-actions {
    width: 100%;
    flex-direction: row;

    .start-button {
      flex: 1;
    }
  }

  .compact-mode .card-actions {
    width: auto;
  }
}
</style>
