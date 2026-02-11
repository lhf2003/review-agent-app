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
        <el-icon :size="24">
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
          <span class="pill-tag difficulty" :class="collection.difficulty || 'medium'">
            {{ difficultyLabel }}
          </span>
          <span class="pill-tag count" v-if="collection.questionCount">
            {{ collection.questionCount }} 题
          </span>
          <span class="pill-tag match" v-if="matchRate > 0">
            匹配度 {{ matchRate }}%
          </span>
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
      <div class="action-wrapper">
        <el-button
          type="primary"
          circle
          :icon="ArrowRight"
          @click.stop="handleStartLearning"
          class="start-button"
        />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.recommendation-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  position: relative;
  overflow: hidden;

  &:hover {
    background: rgba(255, 255, 255, 0.8);
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
    border-color: rgba(255, 255, 255, 0.8);

    .collection-icon {
      transform: scale(1.05);
      background: var(--el-color-primary);
      color: white;
    }
    
    .start-button {
      transform: scale(1.1);
      background: var(--el-color-primary);
      color: white;
    }
  }
}

// 左侧区域
.card-left {
  flex: 1;
  display: flex;
  align-items: flex-start;
  gap: 14px;
  min-width: 0;
}

.collection-icon {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  transition: all 0.3s ease;
}

.collection-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.collection-name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.collection-desc {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.tags-section {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 2px;
}

// Pill Tags
.pill-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  line-height: 16px;
  
  &.difficulty {
    &.easy { background: rgba(103, 194, 58, 0.1); color: #67c23a; }
    &.medium { background: rgba(64, 158, 255, 0.1); color: #409eff; }
    &.hard { background: rgba(230, 162, 60, 0.1); color: #e6a23c; }
    &.expert { background: rgba(245, 108, 108, 0.1); color: #f56c6c; }
  }
  
  &.count {
    background: rgba(144, 147, 153, 0.1);
    color: var(--el-text-color-secondary);
  }
  
  &.match {
    background: rgba(64, 158, 255, 0.1);
    color: var(--el-color-primary);
    font-weight: 600;
  }
}

// 右侧区域
.card-right {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  gap: 8px;
  height: 100%;
}

.recommendation-reason {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: rgba(230, 162, 60, 0.1);
  border-radius: 6px;
  max-width: 120px;
}

.reason-icon {
  font-size: 12px;
  color: #e6a23c;
  flex-shrink: 0;
}

.reason-text {
  font-size: 11px;
  color: #e6a23c;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.start-button {
  background: rgba(0, 0, 0, 0.04);
  border: none;
  color: var(--el-text-color-secondary);
  width: 32px;
  height: 32px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  
  &:hover {
    background: var(--el-color-primary);
    color: white;
  }
}

// 深色模式适配
:global(.dark) {
  .recommendation-card {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.05);

    &:hover {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.1);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
    }
  }
  
  .collection-icon {
    background: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.6);
  }
  
  .start-button {
    background: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.6);
    
    &:hover {
      background: var(--el-color-primary);
      color: white;
    }
  }
  
  .pill-tag {
    &.count {
      background: rgba(255, 255, 255, 0.1);
      color: rgba(255, 255, 255, 0.6);
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .recommendation-card {
    padding: 12px;
    gap: 12px;
  }
  
  .collection-icon {
    width: 40px;
    height: 40px;
  }
  
  .card-right {
    justify-content: center;
  }
  
  .recommendation-reason {
    display: none; // 移动端简化显示
  }
}
</style>
