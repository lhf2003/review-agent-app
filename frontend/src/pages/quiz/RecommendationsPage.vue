<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import LearningPathRecommender from '../../components/quiz/LearningPathRecommender.vue'
import ReviewCard from '../../components/quiz/ReviewCard.vue'
import CustomScroll from '../../components/CustomScroll.vue'
import { api } from '../../api/http'
import { ElMessage } from 'element-plus'

/**
 * 智能推荐页面
 * 左侧：学习路径推荐（基于薄弱知识点推荐合集）
 * 右侧：复习推荐卡片列表（基于艾宾浩斯遗忘曲线推荐复习）
 */

const emit = defineEmits(['recommendations-loaded', 'start-review'])

const router = useRouter()

// 数据状态
const loading = ref(false)
const recommendations = ref([])

// 组件引用
const learningPathRef = ref(null)

// 加载复习推荐列表
async function loadReviewRecommendations() {
  loading.value = true
  try {
    const data = await api.getReviewRecommendation()
    recommendations.value = data
    emit('recommendations-loaded', data.length)
  } catch (error) {
    ElMessage.error('加载复习推荐失败')
  } finally {
    loading.value = false
  }
}

// 开始复习
function handleStartReview(rec) {
  emit('start-review', rec)
}

// 忽略推荐
function handleDismiss(rec) {
  const index = recommendations.value.findIndex(r => r.mistakeId === rec.mistakeId)
  if (index !== -1) {
    recommendations.value.splice(index, 1)
    emit('recommendations-loaded', recommendations.value.length)
  }
  ElMessage.success('已忽略该推荐')
}

// 开始学习（来自学习路径推荐）
function handleStartLearning(collection) {
  router.push({
    path: `/collections/${collection.id}`
  })
}

// 刷新推荐
async function refresh() {
  await loadReviewRecommendations()
  if (learningPathRef.value) {
    await learningPathRef.value.refresh()
  }
  ElMessage.success('推荐已更新')
}

// 组件挂载时加载数据
onMounted(() => {
  loadReviewRecommendations()
})

// 暴露刷新方法
defineExpose({
  refresh
})
</script>

<template>
  <div class="recommendations-page">
    <!-- 左侧：学习路径推荐 -->
    <CustomScroll class="left-pane glass-panel" :hide-scrollbar="true">
      <LearningPathRecommender
        ref="learningPathRef"
        :limit="8"
        :auto-load="true"
      />
    </CustomScroll>

    <!-- 右侧：复习推荐卡片列表 -->
    <div class="right-pane glass-panel">
      <!-- 固定顶部栏 -->
      <div class="section-header">
        <div class="header-content">
          <h3>复习推荐</h3>
          <p class="section-subtitle">基于艾宾浩斯遗忘曲线的智能复习计划</p>
        </div>
        <div class="header-decoration">
          <el-tag effect="dark" round type="primary" class="count-tag" v-if="recommendations.length > 0">
            {{ recommendations.length }} 项待复习
          </el-tag>
        </div>
      </div>

      <CustomScroll class="review-recommendations">
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <el-skeleton
            v-for="i in 3"
            :key="i"
            animated
            class="skeleton-item"
          >
            <template #template>
              <div style="display: flex; gap: 16px; align-items: center;">
                <el-skeleton-item variant="circle" style="width: 48px; height: 48px" />
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
          <el-empty
            description="暂无复习推荐，继续答题后系统会为您智能推荐"
            :image-size="120"
          >
            <el-button
              type="primary"
              @click="() => router.push('/collections')"
              class="action-button"
            >
              开始做题
            </el-button>
          </el-empty>
        </div>

        <!-- 推荐卡片列表 -->
        <div v-else class="review-cards-list">
          <ReviewCard
            v-for="rec in recommendations"
            :key="rec.mistakeId"
            :recommendation="rec"
            @start-review="handleStartReview"
            @dismiss="handleDismiss"
          />
        </div>
      </CustomScroll>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '../../styles/nebula-theme.scss' as *;

.recommendations-page {
  display: grid;
  grid-template-columns: 40% 1fr;
  gap: 20px;
  height: 100%;
  min-height: 0;
  padding: 4px;
}

// 左侧面板
.left-pane {
  height: 100%;
  border-radius: 20px;
  padding: 20px;
}

// 右侧面板
.right-pane {
  height: 100%;
  overflow: hidden;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
}

.review-recommendations {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 0 24px 24px 24px;
  gap: 24px;
}

// 标题区域 - 固定顶部栏样式（参考 QuizDetailPage.vue）
.section-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px 24px;
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur)) saturate(180%);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(180%);
  border-bottom: 1px solid var(--glass-border);
  transition: background 0.3s ease;

  .header-content {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  h3 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    letter-spacing: -0.01em;
    color: var(--text-primary);
  }

  .section-subtitle {
    margin: 0;
    font-size: 13px;
    color: var(--text-secondary);
  }

  .count-tag {
    font-weight: 600;
    letter-spacing: 0.5px;
  }
}

// 加载状态
.loading-state {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-item {
  padding: 20px;
  background: var(--glass-surface);
  border-radius: 16px;
}

// 空状态
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  
  .action-button {
    padding: 10px 24px;
    height: auto;
    font-weight: 600;
    letter-spacing: 0.5px;
  }
}

// 推荐卡片列表
.review-cards-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  min-height: 0;
  padding-bottom: 20px;
}

// 玻璃态效果 - Nebula Theme
.glass-panel {
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur)) saturate(180%);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(180%);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-sm);
  /* Remove transition: all to prevent scale effect on theme switch */
  transition: box-shadow 0.3s ease, transform 0.3s ease !important;
}

// 响应式设计
@media (max-width: 1024px) {
  .recommendations-page {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
    gap: 16px;
    overflow-y: auto;
  }

  .left-pane {
    max-height: 400px;
    // 移动端可能需要滚动条
    scrollbar-width: thin;
    &::-webkit-scrollbar {
      display: block;
      width: 4px;
    }
  }

  .right-pane {
    min-height: 500px;
  }
}

@media (max-width: 768px) {
  .recommendations-page {
    padding: 0;
    gap: 12px;
  }

  .left-pane,
  .right-pane {
    border-radius: 16px;
    padding: 16px;
  }

  .review-recommendations {
    padding: 16px;
    gap: 16px;
  }

  .section-header {
    padding: 12px 16px;

    h3 {
      font-size: 18px;
    }

    .section-subtitle {
      font-size: 12px;
    }

    .count-tag {
      display: none; // 移动端空间不足时隐藏
    }
  }

  .review-recommendations {
    padding: 0 16px 16px 16px;
  }
}
</style>
