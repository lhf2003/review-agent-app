<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { School, TrendCharts, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import RecommendationCard from './RecommendationCard.vue'
import { api } from '../../api/http'

/**
 * 学习路径推荐引擎组件
 * 基于用户薄弱知识点智能推荐学习合集
 */
const props = defineProps({
  // 限制推荐数量
  limit: {
    type: Number,
    default: 5
  },
  // 是否自动加载
  autoLoad: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['recommendations-loaded'])

const router = useRouter()

// 数据状态
const loading = ref(false)
const collections = ref([])
const recommendations = ref([])

// 加载推荐合集
async function loadRecommendations() {
  loading.value = true
  try {
    // 获取所有合集
    const allCollections = await api.getCollections()

    // 获取用户薄弱知识点
    const weakPoints = await getWeakKnowledgePoints()

    // 为每个合集计算匹配度
    recommendations.value = allCollections
      .map(collection => {
        const matchInfo = calculateMatch(collection, weakPoints)
        return {
          ...collection,
          matchRate: matchInfo.rate,
          reason: matchInfo.reason
        }
      })
      .filter(rec => rec.matchRate > 0) // 只显示匹配度大于0的
      .sort((a, b) => b.matchRate - a.matchRate) // 按匹配度排序
      .slice(0, props.limit)

    emit('recommendations-loaded', recommendations.value)
  } catch (e) {
    console.error('加载推荐失败:', e)
    ElMessage.error('加载推荐失败')
  } finally {
    loading.value = false
  }
}

// 获取薄弱知识点（模拟数据，实际应从后端获取）
async function getWeakKnowledgePoints() {
  try {
    // 尝试从统计API获取知识点掌握度
    const stats = await api.getUserStats()
    return stats.knowledgeMastery || []
  } catch (e) {
    // 如果API不可用，返回空数组
    return []
  }
}

// 计算合集与薄弱知识点的匹配度
function calculateMatch(collection, weakPoints) {
  if (!weakPoints || weakPoints.length === 0) {
    return { rate: 0, reason: '' }
  }

  let matchRate = 0
  let matchedPoints = []

  // 假设合集有 tags 字段包含知识点
  const collectionTags = collection.tags || []

  weakPoints.forEach(point => {
    const knowledgePoint = point.knowledgePoint
    const masteryRate = point.masteryRate || 0

    // 如果合集包含该知识点标签
    if (collectionTags.some(tag =>
      tag.toLowerCase().includes(knowledgePoint.toLowerCase()) ||
      knowledgePoint.toLowerCase().includes(tag.toLowerCase())
    )) {
      // 掌握度越低，匹配度越高
      const contribution = (100 - masteryRate) * 0.6
      matchRate += contribution
      matchedPoints.push(knowledgePoint)
    }
  })

  // 限制匹配度在 0-100 之间
  matchRate = Math.min(Math.round(matchRate), 100)

  // 生成推荐原因
  let reason = ''
  if (matchedPoints.length > 0) {
    if (matchRate >= 80) {
      reason = `涵盖你的薄弱点：${matchedPoints.slice(0, 2).join('、')}`
    } else if (matchRate >= 50) {
      reason = `相关知识点：${matchedPoints[0]}`
    } else {
      reason = '建议巩固基础'
    }
  }

  return { rate: matchRate, reason }
}

// 开始学习
function handleStartLearning(collection) {
  router.push({
    path: `/collections/${collection.id}`
  })
}

// 刷新推荐
async function refresh() {
  await loadRecommendations()
  ElMessage.success('推荐已更新')
}

// 组件挂载时加载
onMounted(() => {
  if (props.autoLoad) {
    loadRecommendations()
  }
})

// 暴露刷新方法
defineExpose({
  refresh,
  loadRecommendations
})
</script>

<template>
  <div class="learning-path-recommender">
    <!-- 头部 -->
    <div class="recommender-header">
      <div class="header-left">
        <div class="header-icon">
          <el-icon :size="24"><School /></el-icon>
        </div>
        <div class="header-text">
          <h3 class="header-title">智能学习路径</h3>
          <p class="header-subtitle">基于你的薄弱知识点推荐</p>
        </div>
      </div>
      <div class="header-actions">
        <el-button
          :icon="RefreshRight"
          :loading="loading"
          @click="refresh"
          circle
        />
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-skeleton
        v-for="i in 3"
        :key="i"
        animated
        class="skeleton-item"
      >
        <template #template>
          <div style="display: flex; gap: 16px; align-items: center;">
            <el-skeleton-item variant="circle" style="width: 56px; height: 56px" />
            <div style="flex: 1">
              <el-skeleton-item variant="text" style="width: 60%" />
              <el-skeleton-item variant="text" style="width: 40%; margin-top: 8px" />
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>

    <!-- 空状态 -->
    <div v-else-if="recommendations.length === 0" class="empty-state">
      <el-icon class="empty-icon" :size="64">
        <TrendCharts />
      </el-icon>
      <div class="empty-text">
        <h4>暂无推荐</h4>
        <p>继续学习后，系统将为你智能推荐学习路径</p>
      </div>
    </div>

    <!-- 推荐列表 -->
    <div v-else class="recommendations-list">
      <RecommendationCard
        v-for="rec in recommendations"
        :key="rec.id"
        :collection="rec"
        :reason="rec.reason"
        :match-rate="rec.matchRate"
        @start-learning="handleStartLearning"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.learning-path-recommender {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

// 头部
.recommender-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, var(--el-color-primary-light-9) 0%, var(--el-bg-color) 100%);
  border-radius: 16px;
  border: 1px solid var(--el-color-primary-light-5);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--el-color-primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.header-subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.header-actions {
  display: flex;
  gap: 8px;
}

// 加载状态
.loading-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 0;
}

.skeleton-item {
  padding: 20px;
  background: var(--el-bg-color);
  border-radius: 16px;
  border: 1px solid var(--el-border-color-light);
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  background: var(--el-bg-color);
  border-radius: 16px;
  border: 1px dashed var(--el-border-color);
}

.empty-icon {
  color: var(--el-text-color-placeholder);
  opacity: 0.6;
}

.empty-text {
  text-align: center;

  h4 {
    margin: 0 0 8px 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  p {
    margin: 0;
    font-size: 14px;
    color: var(--el-text-color-secondary);
  }
}

// 推荐列表
.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

// 深色模式适配
:global(.dark) .recommender-header {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.15) 0%, rgba(40, 40, 40, 0.8) 100%);
  border-color: rgba(64, 158, 255, 0.3);
}

:global(.dark) .header-icon {
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}

// 响应式设计
@media (max-width: 768px) {
  .recommender-header {
    padding: 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .empty-state {
    padding: 40px 20px;
  }
}
</style>
