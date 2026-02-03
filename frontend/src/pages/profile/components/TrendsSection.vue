<template>
  <div class="trends-section">
    <!-- 测验分数趋势图 -->
    <div class="chart-section">
      <h4 class="section-title">
        <el-icon><TrendCharts /></el-icon>
        测验分数趋势
      </h4>
      <ScoreTrendChart
        ref="scoreTrendChartRef"
        :data="quizScoreTrend"
        :loading="loading.trends"
        height="300px"
      />
    </div>

    <!-- 知识点掌握度分布 -->
    <div class="chart-section">
      <h4 class="section-title">
        <el-icon><Star /></el-icon>
        知识点掌握度
      </h4>
      <KnowledgeRadarChart
        ref="knowledgeRadarChartRef"
        :data="knowledgeMastery"
        :loading="loading.trends"
        height="300px"
      />
    </div>

    <!-- 薄弱知识点列表 -->
    <div class="weakness-section">
      <h4 class="section-title">
        <el-icon><PriceTag /></el-icon>
        薄弱知识点
      </h4>
      <WeaknessList
        :knowledge-data="knowledgeMastery"
        :limit="5"
        :threshold="60"
        @start-practice="handleStartPractice"
      />
    </div>

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
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { TrendCharts, Star, PriceTag } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import WeaknessList from '../../../components/quiz/WeaknessList.vue'
import { ScoreTrendChart, KnowledgeRadarChart, ProgressGaugeChart } from '../../../components/charts'

const router = useRouter()

// 图表引用
const scoreTrendChartRef = ref(null)
const knowledgeRadarChartRef = ref(null)
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

// 当组件可见时，触发所有图表 resize
function resizeAllCharts() {
  setTimeout(() => {
    scoreTrendChartRef.value?.resize()
    knowledgeRadarChartRef.value?.resize()
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

const quizScoreTrend = computed(() => props.achievementsData?.quizScoreTrend || [])
const knowledgeMastery = computed(() => props.achievementsData?.knowledgeMastery || [])
const learningProgress = computed(() => props.achievementsData?.learningProgress || {
  overallProgress: 0,
  syncProgress: 0,
  analysisProgress: 0,
  quizProgress: 0,
  achievementProgress: 0
})

// 开始练习薄弱知识点
function handleStartPractice(point) {
  // 跳转到合集页面，并传递知识点筛选
  router.push({
    path: '/collections',
    query: { tag: point.tagName }
  })
}

// 查看相关合集
function viewRelatedCollections(point) {
  router.push({
    path: '/collections',
    query: { tag: point.tagName }
  })
}
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

.chart-section {
  margin-bottom: 40px;
}

.weakness-section {
  margin-bottom: 40px;
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
</style>
