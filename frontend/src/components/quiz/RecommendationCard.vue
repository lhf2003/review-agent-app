<script setup>
import { computed } from 'vue'
import { FolderOpened, Star, TrendCharts, ArrowRight } from '@element-plus/icons-vue'

/**
 * 推荐合集卡片组件
 * 基于用户薄弱知识点智能推荐学习合集
 */
const props = defineProps({
  // 推荐合集数据
  collection: {
    type: Object,
    required: true,
    validator: (value) => {
      return value &&
             typeof value.id !== 'undefined' &&
             typeof value.name !== 'undefined'
    }
  },
  // 推荐原因
  reason: {
    type: String,
    default: ''
  },
  // 匹配度（0-100）
  matchRate: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['start-learning'])

// 计算难度颜色
const difficultyColor = computed(() => {
  const colors = {
    'easy': '#67c23a',
    'medium': '#409eff',
    'hard': '#e6a23c',
    'expert': '#f56c6c'
  }
  return colors[props.collection.difficulty] || '#909399'
})

// 计算难度标签
const difficultyLabel = computed(() => {
  const labels = {
    'easy': '简单',
    'medium': '中等',
    'hard': '困难',
    'expert': '专家'
  }
  return labels[props.collection.difficulty] || '未知'
})

// 开始学习
function handleStartLearning() {
  emit('start-learning', props.collection)
}
</script>

<template>
  <div class="recommendation-card" @click="handleStartLearning">
    <!-- 左侧：合集信息 -->
    <div class="card-left">
      <div class="collection-icon">
        <el-icon :size="28">
          <FolderOpened />
        </el-icon>
      </div>

      <div class="collection-info">
        <h4 class="collection-name">{{ collection.name }}</h4>
        <p v-if="collection.description" class="collection-desc">
          {{ collection.description }}
        </p>

        <!-- 标签区域 -->
        <div class="tags-section">
          <el-tag
            :color="difficultyColor"
            effect="dark"
            size="small"
          >
            {{ difficultyLabel }}
          </el-tag>
          <el-tag
            v-if="collection.questionCount"
            type="info"
            size="small"
            effect="plain"
          >
            {{ collection.questionCount }} 题
          </el-tag>
          <el-tag
            v-if="matchRate > 0"
            type="success"
            size="small"
            effect="plain"
          >
            匹配度 {{ matchRate }}%
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 右侧：推荐原因和操作 -->
    <div class="card-right">
      <!-- 推荐原因 -->
      <div v-if="reason" class="recommendation-reason">
        <el-icon class="reason-icon"><Star /></el-icon>
        <span class="reason-text">{{ reason }}</span>
      </div>

      <!-- 开始学习按钮 -->
      <el-button
        type="primary"
        :icon="ArrowRight"
        @click.stop="handleStartLearning"
        class="start-button"
      >
        开始学习
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.recommendation-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px;
  background: var(--el-bg-color);
  border-radius: 16px;
  border: 2px solid var(--el-border-color-light);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);

  &:hover {
    transform: translateY(-3px) translateX(4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    border-color: var(--el-color-primary-light-5);

    .start-button {
      transform: translateX(4px);
    }

    .collection-icon {
      transform: scale(1.1);
      background: var(--el-color-primary-light-9);
    }
  }
}

// 左侧区域
.card-left {
  flex: 1;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  min-width: 0;
}

.collection-icon {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-color-primary);
  transition: all 0.3s ease;
}

.collection-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.collection-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.collection-desc {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tags-section {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

// 右侧区域
.card-right {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

.recommendation-reason {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: var(--el-color-warning-light-9);
  border-radius: 8px;
  border-left: 3px solid var(--el-color-warning);
  max-width: 200px;
}

.reason-icon {
  font-size: 18px;
  color: var(--el-color-warning);
  flex-shrink: 0;
}

.reason-text {
  font-size: 13px;
  color: var(--el-text-color-regular);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.start-button {
  transition: transform 0.3s ease;
  white-space: nowrap;
}

// 深色模式适配
:global(.dark) .recommendation-card {
  &:hover {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  }

  .recommendation-reason {
    background: rgba(230, 162, 60, 0.15);
  }
}

// 响应式设计
@media (max-width: 768px) {
  .recommendation-card {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .card-left {
    width: 100%;
  }

  .card-right {
    width: 100%;
    align-items: stretch;
    flex-direction: row;
    justify-content: space-between;
  }

  .recommendation-reason {
    flex: 1;
    max-width: none;
  }

  .start-button {
    width: auto;
  }
}
</style>
