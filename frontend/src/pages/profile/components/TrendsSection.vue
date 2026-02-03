<template>
  <div class="trends-section">
    <!-- 学习进度 -->
    <div class="progress-section">
      <h4 class="section-title">
        <el-icon><Star /></el-icon>
        学习进度
      </h4>
      <div v-if="loading.progress" v-loading="true" class="loading-container">
        <el-skeleton :rows="2" animated />
      </div>
      <div v-else class="chart-container-wrapper">
        <div class="progress-grid">
          <!-- 环形进度图 -->
          <div class="progress-gauge-wrapper">
            <ProgressGaugeChart
              ref="progressGaugeChartRef"
              :value="learningProgress.overallProgress"
              :loading="loading.progress"
              size="160px"
              label="总体进度"
            />
          </div>

          <!-- 各维度进度条 -->
          <div class="progress-bars">
            <div class="progress-item">
              <div class="progress-label">
                <span>同步文件</span>
                <span class="progress-value">{{ learningProgress.syncProgress }}%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: learningProgress.syncProgress + '%' }"></div>
              </div>
            </div>
            <div class="progress-item">
              <div class="progress-label">
                <span>已分析结果</span>
                <span class="progress-value">{{ learningProgress.analysisProgress }}%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: learningProgress.analysisProgress + '%' }"></div>
              </div>
            </div>
            <div class="progress-item">
              <div class="progress-label">
                <span>测验完成</span>
                <span class="progress-value">{{ learningProgress.quizProgress }}%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: learningProgress.quizProgress + '%' }"></div>
              </div>
            </div>
            <div class="progress-item">
              <div class="progress-label">
                <span>成就解锁</span>
                <span class="progress-value">{{ learningProgress.achievementProgress }}%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: learningProgress.achievementProgress + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计图表 (Moved from QuizHistoryPage) -->
    <div v-if="quizStats.quizScoreTrend.length > 0 || quizStats.knowledgeMastery.length > 0" class="stats-section">
      <div class="section-divider"></div>
      <div class="stats-grid">
        <!-- 分数趋势图 -->
        <div class="stat-card glass-card" v-if="quizStats.quizScoreTrend.length > 0">
          <h4 class="section-title">
            <el-icon><TrendCharts /></el-icon>
            分数趋势
          </h4>
          <ScoreTrendChart
            :data="quizStats.quizScoreTrend"
            :loading="statsLoading"
            height="240px"
          />
        </div>

        <!-- 知识点掌握度 -->
        <div class="stat-card glass-card" v-if="quizStats.knowledgeMastery.length > 0">
          <h4 class="section-title">
            <el-icon><Star /></el-icon>
            知识点掌握度
          </h4>
          <KnowledgeRadarChart
            :data="quizStats.knowledgeMastery"
            :loading="statsLoading"
            height="240px"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { Star, TrendCharts } from '@element-plus/icons-vue'
import { ProgressGaugeChart, ScoreTrendChart, KnowledgeRadarChart } from '../../../components/charts'
import { api } from '../../../api/http'

// 图表引用
const progressGaugeChartRef = ref(null)

const props = defineProps({
  achievementsData: {
    type: Object,
    required: true
  },
  loading: {
    type: Object,
    default: () => ({ achievements: false, trends: false, progress: false })
  },
  cardsVisible: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['charts-ready'])

// 统计数据
const quizStats = ref({
  quizScoreTrend: [],
  knowledgeMastery: []
})
const statsLoading = ref(false)

/**
 * 加载统计数据
 */
async function loadStats() {
  statsLoading.value = true
  try {
    const data = await api.getQuizStats()
    quizStats.value = {
      quizScoreTrend: data.quizScoreTrend || [],
      knowledgeMastery: data.knowledgeMastery || []
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  } finally {
    statsLoading.value = false
  }
}

// 当组件可见时，触发所有图表 resize
function resizeAllCharts() {
  setTimeout(() => {
    progressGaugeChartRef.value?.resize()
  }, 150)
}

// 监听数据变化，数据更新后也 resize
watch(() => props.achievementsData, () => {
  if (props.achievementsData && Object.keys(props.achievementsData).length > 0) {
    setTimeout(() => {
      resizeAllCharts()
    }, 200)
  }
}, { deep: true })

onMounted(() => {
  loadStats()
  // 组件挂载后延迟 resize
  setTimeout(() => {
    resizeAllCharts()
    emit('charts-ready')
  }, 300)
})

// 暴露方法给父组件
defineExpose({
  resizeAllCharts
})

const learningProgress = computed(() => props.achievementsData?.learningProgress || {
  overallProgress: 0,
  syncProgress: 0,
  analysisProgress: 0,
  quizProgress: 0,
  achievementProgress: 0
})
</script>

<style scoped>
.trends-section {
  padding: 32px;
  background: rgba(255, 255, 255, 0.65);
  border-radius: 24px;
  backdrop-filter: blur(24px) saturate(180%);
  box-shadow:
    0 20px 40px rgba(0, 0, 0, 0.05),
    0 1px 2px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  border: none;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  box-sizing: border-box;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 24px 0;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: -0.01em;
  color: var(--el-text-color-primary);
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Helvetica Neue", sans-serif;
}

.section-title .el-icon {
  font-size: 22px;
  color: var(--el-color-primary);
  filter: drop-shadow(0 2px 4px rgba(var(--el-color-primary-rgb), 0.2));
}

.chart-container-wrapper {
  position: relative;
  height: 300px;
}

.progress-section {
  margin-bottom: 0;
}

.progress-grid {
  display: flex;
  gap: 40px;
  align-items: center;
}

.progress-gauge-wrapper {
  display: flex;
  flex: 0 0 160px;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.progress-bars {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.progress-item {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.progress-item .progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.progress-value {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--el-text-color-primary);
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 100px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--el-color-primary) 0%, color-mix(in srgb, var(--el-color-primary), white 20%) 100%);
  border-radius: 100px;
  transition: width 0.6s cubic-bezier(0.25, 1, 0.5, 1);
  box-shadow: 0 2px 4px rgba(var(--el-color-primary-rgb), 0.2);
}

/* Stats Grid */
.section-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 32px 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
}

.stat-card {
  /* Inherit some styles or add specific ones if needed, but glass-card class is not defined here.
     The parent .trends-section is already a glass card.
     We can just make these transparent or slightly distinct.
  */
  background: rgba(255, 255, 255, 0.4);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

/* Dark Mode */
html.dark .trends-section {
  background: rgba(30, 30, 30, 0.65);
  box-shadow:
    0 20px 40px rgba(0, 0, 0, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
}

html.dark .progress-bar {
  background: rgba(255, 255, 255, 0.08);
}

html.dark .progress-fill {
  background: linear-gradient(90deg, var(--el-color-primary) 0%, color-mix(in srgb, var(--el-color-primary), black 10%) 100%);
  box-shadow: 0 2px 8px rgba(var(--el-color-primary-rgb), 0.3);
}

html.dark .section-divider {
  background: rgba(255, 255, 255, 0.1);
}

html.dark .stat-card {
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.05);
}
</style>
