<script setup>
import { computed } from 'vue'
import DataFileCard from './DataFileCard.vue'
import CustomScroll from '../CustomScroll.vue'
import { ElEmpty, ElSkeleton, ElSkeletonItem } from 'element-plus'

/**
 * DataFileGrid - 文件卡片网格容器
 * 响应式布局：桌面4列 → 平板3列 → 手机2列 → 小手机1列
 * 
 * @author Review Agent
 * @version 1.0.0
 */
const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  emptyText: {
    type: String,
    default: '暂无数据'
  },
  emptyDescription: {
    type: String,
    default: '导入文件后将在此处显示'
  }
})

const emit = defineEmits([
  'view-content',
  'analyze',
  'view-result',
  'delete',
  'retry'
])

// 骨架屏数量
const skeletonCount = 8

// 处理卡片事件
function handleViewContent(item) {
  emit('view-content', item)
}

function handleAnalyze(item) {
  emit('analyze', item)
}

function handleViewResult(item) {
  emit('view-result', item)
}

function handleDelete(item) {
  emit('delete', item)
}

function handleRetry(item) {
  emit('retry', item)
}
</script>

<template>
  <div class="data-file-grid-container">
    <CustomScroll class="grid-scroll-wrapper">
      <!-- 加载状态：骨架屏 -->
      <div v-if="loading" class="grid-skeleton">
        <div
          v-for="i in skeletonCount"
          :key="i"
          class="skeleton-card"
        >
          <ElSkeleton animated>
            <template #template>
              <div class="skeleton-content">
                <div class="skeleton-header">
                  <ElSkeletonItem variant="circle" style="width: 40px; height: 40px" />
                  <ElSkeletonItem variant="text" style="width: 60px; height: 24px" />
                </div>
                <ElSkeletonItem variant="text" style="width: 90%; margin-top: 16px" />
                <ElSkeletonItem variant="text" style="width: 60%; margin-top: 8px" />
                <div class="skeleton-footer">
                  <ElSkeletonItem variant="text" style="width: 40%; height: 36px" />
                  <ElSkeletonItem variant="text" style="width: 36px; height: 36px; border-radius: 50%" />
                </div>
              </div>
            </template>
          </ElSkeleton>
        </div>
      </div>

      <!-- 数据网格 -->
      <transition-group
        v-else-if="data.length > 0"
        name="card-fade"
        tag="div"
        class="data-grid"
      >
        <DataFileCard
          v-for="item in data"
          :key="item.id"
          :data="item"
          @view-content="handleViewContent"
          @analyze="handleAnalyze"
          @view-result="handleViewResult"
          @delete="handleDelete"
          @retry="handleRetry"
        />
      </transition-group>

      <!-- 空状态 -->
      <div v-else class="empty-wrapper">
        <ElEmpty
          :description="emptyText"
          :image-size="140"
        >
          <template #description>
            <div class="empty-content">
              <p class="empty-title">{{ emptyText }}</p>
              <p v-if="emptyDescription" class="empty-desc">{{ emptyDescription }}</p>
            </div>
          </template>
        </ElEmpty>
      </div>
    </CustomScroll>
  </div>
</template>

<style scoped lang="scss">
.data-file-grid-container {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

.grid-scroll-wrapper {
  height: 100%;
  padding: 4px;
}

// 数据网格布局
.data-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 8px 12px 24px 4px;
  
  // 平板：3列
  @media (max-width: 1200px) {
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }
  
  // 小平板：2列
  @media (max-width: 900px) {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  
  // 手机：2列（紧凑）
  @media (max-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 4px 8px 16px 0;
  }
  
  // 小手机：1列
  @media (max-width: 400px) {
    grid-template-columns: 1fr;
    gap: 12px;
  }
}

// 骨架屏网格
.grid-skeleton {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  padding: 8px 12px 24px 4px;
  
  @media (max-width: 1200px) {
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }
  
  @media (max-width: 900px) {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  
  @media (max-width: 640px) {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  
  @media (max-width: 400px) {
    grid-template-columns: 1fr;
  }
}

.skeleton-card {
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border-radius: 20px;
  padding: 16px;
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
}

.skeleton-content {
  display: flex;
  flex-direction: column;
}

.skeleton-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.skeleton-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
}

// 空状态
.empty-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  height: 100%;
}

.empty-content {
  text-align: center;
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

// 卡片淡入动画
.card-fade-enter-active,
.card-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
}

.card-fade-enter-from {
  opacity: 0;
  transform: translateY(20px) scale(0.95);
}

.card-fade-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

.card-fade-move {
  transition: transform 0.3s ease;
}

// 减少动画偏好
@media (prefers-reduced-motion: reduce) {
  .card-fade-enter-active,
  .card-fade-leave-active,
  .card-fade-move {
    transition: none;
  }
}
</style>