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

// 加载推荐合集（直接调用后端推荐接口）
async function loadRecommendations() {
  loading.value = true
  try {
    // 直接调用后端推荐接口
    const data = await api.getCollectionRecommendations(props.limit)

    // 映射数据结构以匹配前端组件期望
    recommendations.value = data.map(rec => ({
      ...rec,
      // 字段映射：count -> questionCount
      questionCount: rec.count,
      // 补充缺失字段：difficulty 根据题目数量或匹配度计算
      difficulty: calculateDifficulty(rec)
    }))

    emit('recommendations-loaded', recommendations.value)
  } catch {
    ElMessage.error('加载推荐失败')
  } finally {
    loading.value = false
  }
}

// 根据合集数据计算难度级别
function calculateDifficulty(rec) {
  const matchRate = rec.matchRate || 0
  const count = rec.count || 0

  // 根据匹配度和题目数量综合判断难度
  if (matchRate >= 80 || count >= 20) return 'expert'
  if (matchRate >= 60 || count >= 15) return 'hard'
  if (matchRate >= 40 || count >= 10) return 'medium'
  return 'easy'
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
        <div class="header-icon-wrapper">
          <el-icon :size="20" class="header-icon"><School /></el-icon>
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
          class="refresh-btn"
          plain
        />
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div v-for="i in 3" :key="i" class="skeleton-card">
        <el-skeleton animated>
          <template #template>
            <div style="display: flex; gap: 16px; align-items: center;">
              <el-skeleton-item variant="rect" style="width: 48px; height: 48px; border-radius: 12px" />
              <div style="flex: 1">
                <el-skeleton-item variant="text" style="width: 60%" />
                <el-skeleton-item variant="text" style="width: 40%; margin-top: 8px" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="recommendations.length === 0" class="empty-state">
      <div class="empty-icon-wrapper">
        <el-icon :size="32"><TrendCharts /></el-icon>
      </div>
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
  height: 100%;
}

// 头部
.recommender-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0 16px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--el-color-primary) 0%, var(--el-color-primary-light-3) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.3);
}

.header-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  letter-spacing: -0.01em;
}

.header-subtitle {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.header-actions {
  display: flex;
  gap: 8px;
  
  .refresh-btn {
    border: none;
    background: rgba(0, 0, 0, 0.04);
    color: var(--el-text-color-secondary);
    transition: all 0.3s ease;
    
    &:hover {
      background: rgba(0, 0, 0, 0.08);
      color: var(--el-color-primary);
      transform: rotate(180deg);
    }
  }
}

// 加载状态
.loading-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 0;
}

.skeleton-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  border: 1px dashed rgba(0, 0, 0, 0.1);
  margin-top: 20px;
}

.empty-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  margin-bottom: 8px;
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
    font-size: 13px;
    color: var(--el-text-color-secondary);
    max-width: 200px;
    line-height: 1.5;
  }
}

// 推荐列表
.recommendations-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 20px;
}

// 深色模式适配
html.dark {
  .recommender-header {
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }

  .header-icon-wrapper {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  }

  .refresh-btn {
    background: rgba(255, 255, 255, 0.1);
    color: rgba(255, 255, 255, 0.7);
    
    &:hover {
      background: rgba(255, 255, 255, 0.2);
      color: white;
    }
  }

  .skeleton-card {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.05);
  }

  .empty-state {
    background: rgba(255, 255, 255, 0.02);
    border-color: rgba(255, 255, 255, 0.1);
  }
  
  .empty-icon-wrapper {
    background: rgba(255, 255, 255, 0.05);
  }
}
</style>
