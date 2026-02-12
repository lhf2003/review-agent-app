<template>
  <div class="recommendation-panel">
    <!-- 面板头部 -->
    <div class="panel-header">
      <div class="header-left">
        <el-icon class="light-icon"><MagicStick /></el-icon>
        <span class="header-title">智能推荐</span>
      </div>
      <el-button
        v-if="recommendations.length > 0"
        link
        type="primary"
        @click="refreshRecommendations"
        :loading="loading"
      >
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading && recommendations.length === 0" class="panel-loading">
      <el-skeleton :rows="3" animated />
    </div>

    <!-- 空状态 -->
    <div v-else-if="recommendations.length === 0" class="panel-empty">
      <el-empty description="暂无推荐" :image-size="80">
        <template #description>
          <p>有更多未归档的分析结果时，会自动推荐创建合集</p>
        </template>
      </el-empty>
    </div>

    <!-- 推荐卡片列表 -->
    <div v-else class="recommendations-list">
      <SmartRecommendationCard
        v-for="rec in visibleRecommendations"
        :key="rec.recommendationId"
        :recommendation="rec"
        @create="handleCreate"
        @dismiss="handleDismiss"
      />

      <!-- 查看更多 -->
      <div v-if="hasMore" class="show-more">
        <el-button link type="primary" @click="showAll = true">
          查看更多推荐 ({{ remainingCount }})
        </el-button>
      </div>
    </div>

    <!-- 统计信息 -->
    <div v-if="totalUnarchived > 0" class="panel-footer">
      <span class="footer-info">
        共 {{ totalUnarchived }} 个未归档的分析结果
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Refresh } from '@element-plus/icons-vue'
import SmartRecommendationCard from './SmartRecommendationCard.vue'
import { collectionApi } from '../../api'

const emit = defineEmits(['created', 'refresh'])

const loading = ref(false)
const recommendations = ref([])
const dismissedIds = ref(new Set())
const totalUnarchived = ref(0)
const showAll = ref(false)

// 最多显示的推荐数量（不展开时）
const maxVisible = 3

// 可见的推荐列表
const visibleRecommendations = computed(() => {
  const filtered = recommendations.value.filter(
    rec => !dismissedIds.value.has(rec.recommendationId)
  )
  if (showAll.value) {
    return filtered
  }
  return filtered.slice(0, maxVisible)
})

// 是否有更多推荐
const hasMore = computed(() => {
  const filtered = recommendations.value.filter(
    rec => !dismissedIds.value.has(rec.recommendationId)
  )
  return filtered.length > maxVisible && !showAll.value
})

// 剩余推荐数量
const remainingCount = computed(() => {
  const filtered = recommendations.value.filter(
    rec => !dismissedIds.value.has(rec.recommendationId)
  )
  return Math.max(0, filtered.length - maxVisible)
})

// 加载推荐数据
async function loadRecommendations() {
  loading.value = true
  try {
    const data = await collectionApi.getSmartRecommendations()
    if (data) {
      recommendations.value = data.recommendations || []
      totalUnarchived.value = data.totalUnarchived || 0
    }
  } catch (e) {
    console.error('加载推荐失败:', e)
  } finally {
    loading.value = false
  }
}

// 刷新推荐
function refreshRecommendations() {
  dismissedIds.value.clear()
  showAll.value = false
  loadRecommendations()
  emit('refresh')
}

// 创建合集
async function handleCreate(data) {
  try {
    const result = await collectionApi.quickCreateCollection(data)
    ElMessage.success(`成功创建合集，包含 ${result.createdCount} 个分析结果`)
    emit('created', result)

    // 从推荐列表中移除
    const index = recommendations.value.findIndex(
      rec => rec.suggestedName === data.name
    )
    if (index !== -1) {
      dismissedIds.value.add(recommendations.value[index].recommendationId)
    }
  } catch (e) {
    ElMessage.error('创建合集失败: ' + e.message)
  }
}

// 忽略推荐
async function handleDismiss(recommendationId) {
  try {
    await collectionApi.dismissRecommendation(recommendationId)
    dismissedIds.value.add(recommendationId)
    ElMessage.success('已忽略该推荐')
  } catch (e) {
    // 即使后端忽略失败，前端也隐藏
    dismissedIds.value.add(recommendationId)
  }
}

onMounted(() => {
  loadRecommendations()
})

// 暴露方法
defineExpose({
  refresh: loadRecommendations
})
</script>

<style scoped lang="scss">
.recommendation-panel {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

html.dark .recommendation-panel {
  background: rgba(28, 28, 30, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

html.dark .panel-header {
  border-bottom-color: rgba(255, 255, 255, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.light-icon {
  font-size: 18px;
  color: var(--el-color-warning);
}

.header-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.panel-loading {
  padding: 20px;
}

.panel-empty {
  padding: 24px;

  p {
    font-size: 13px;
    color: var(--el-text-color-placeholder);
    margin-top: 8px;
  }
}

.recommendations-list {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.show-more {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.panel-footer {
  padding: 12px 20px;
  background: var(--el-fill-color-lighter);
  border-top: 1px solid var(--el-border-color-lighter);
}

html.dark .panel-footer {
  background: rgba(0, 0, 0, 0.15);
  border-top-color: rgba(255, 255, 255, 0.05);
}

.footer-info {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
