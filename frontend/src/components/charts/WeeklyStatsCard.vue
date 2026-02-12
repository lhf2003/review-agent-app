<template>
  <div class="weekly-stats-card">
    <div v-if="loading" class="card-loading">
      <el-skeleton :rows="2" animated />
    </div>
    <template v-else>
      <div class="stat-item">
        <div class="stat-label">测验完成</div>
        <div class="stat-value-group">
          <span class="stat-value">{{ stats.thisWeekQuizCount }}</span>
          <span class="stat-unit">次</span>
          <span
            class="stat-change"
            :class="quizChangeClass"
          >
            {{ quizChangeText }}
          </span>
        </div>
        <div class="stat-compare">上周 {{ stats.lastWeekQuizCount }} 次</div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-label">正确率</div>
        <div class="stat-value-group">
          <span class="stat-value">{{ stats.thisWeekAccuracy.toFixed(1) }}</span>
          <span class="stat-unit">%</span>
          <span
            class="stat-change"
            :class="accuracyChangeClass"
          >
            {{ accuracyChangeText }}
          </span>
        </div>
        <div class="stat-compare">上周 {{ stats.lastWeekAccuracy.toFixed(1) }}%</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  stats: {
    type: Object,
    default: () => ({
      thisWeekQuizCount: 0,
      lastWeekQuizCount: 0,
      thisWeekAccuracy: 0,
      lastWeekAccuracy: 0
    })
  },
  loading: {
    type: Boolean,
    default: false
  }
})

// 测验数量变化
const quizChange = computed(() => {
  return props.stats.thisWeekQuizCount - props.stats.lastWeekQuizCount
})

const quizChangeClass = computed(() => {
  if (quizChange.value > 0) return 'change-up'
  if (quizChange.value < 0) return 'change-down'
  return 'change-same'
})

const quizChangeText = computed(() => {
  if (quizChange.value > 0) return `+${quizChange.value}`
  if (quizChange.value < 0) return `${quizChange.value}`
  return '-'
})

// 正确率变化
const accuracyChange = computed(() => {
  return props.stats.thisWeekAccuracy - props.stats.lastWeekAccuracy
})

const accuracyChangeClass = computed(() => {
  if (accuracyChange.value > 0) return 'change-up'
  if (accuracyChange.value < 0) return 'change-down'
  return 'change-same'
})

const accuracyChangeText = computed(() => {
  if (accuracyChange.value > 0) return `+${accuracyChange.value.toFixed(1)}%`
  if (accuracyChange.value < 0) return `${accuracyChange.value.toFixed(1)}%`
  return '-'
})
</script>

<style scoped lang="scss">
.weekly-stats-card {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  min-height: 100px;
}

html.dark .weekly-stats-card {
  background: rgba(28, 28, 30, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
}

.card-loading {
  width: 100%;
  padding: 8px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}

.stat-value-group {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.stat-unit {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.stat-change {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 6px;
  border-radius: 4px;
  margin-left: 4px;
}

.change-up {
  color: #67C23A;
  background: rgba(103, 194, 58, 0.1);
}

.change-down {
  color: #F56C6C;
  background: rgba(245, 108, 108, 0.1);
}

.change-same {
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
}

.stat-compare {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.stat-divider {
  width: 1px;
  height: 50px;
  background: var(--el-border-color-lighter);
  margin: 0 20px;
}

html.dark .stat-divider {
  background: rgba(255, 255, 255, 0.1);
}
</style>
