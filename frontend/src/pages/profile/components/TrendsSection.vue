<template>
  <div class="trends-section">
    <!-- 测验分数趋势图 -->
    <div class="chart-section">
      <h4 class="section-title">
        <el-icon><TrendCharts /></el-icon>
        测验分数趋势
      </h4>
      <div v-if="loading.trends" v-loading="true" class="loading-container">
        <el-skeleton :rows="3" animated />
      </div>
      <div v-else class="chart-container-wrapper">
        <div ref="scoreTrendChartRef" class="chart"></div>
      </div>
    </div>

    <!-- 知识点掌握度分布 -->
    <div class="chart-section">
      <h4 class="section-title">
        <el-icon><Star /></el-icon>
        知识点掌握度
      </h4>
      <div v-if="loading.trends" v-loading="true" class="loading-container">
        <el-skeleton :rows="1" animated />
      </div>
      <div v-else class="chart-container-wrapper">
        <div ref="masteryRadarChartRef" class="chart"></div>
        <el-empty v-if="!knowledgeMastery || knowledgeMastery.length === 0" description="暂无数据" />
      </div>
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
            <div ref="progressGaugeChartRef" class="progress-gauge"></div>
            <div class="progress-label">总体进度</div>
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
import { defineProps, computed, watch } from 'vue'
import { TrendCharts, Star } from '@element-plus/icons-vue'

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
  },
  // Chart refs and methods from parent
  scoreTrendChartRef: {
    type: Object,
    required: true
  },
  masteryRadarChartRef: {
    type: Object,
    required: true
  },
  progressGaugeChartRef: {
    type: Object,
    required: true
  },
  updateCharts: {
    type: Function,
    required: true
  }
})

const knowledgeMastery = computed(() => props.achievementsData.knowledgeMastery || [])
const learningProgress = computed(() => props.achievementsData.learningProgress || {
  overallProgress: 0,
  syncProgress: 0,
  analysisProgress: 0,
  quizProgress: 0,
  achievementProgress: 0
})

// 监听数据变化，更新图表
watch(() => [props.achievementsData.quizScoreTrend, props.achievementsData.knowledgeMastery, props.achievementsData.learningProgress], () => {
  props.updateCharts()
}, { deep: true })
</script>

<style scoped>
.trends-section {
  padding: 32px;
  background: rgba(255, 255, 255, 0.75);
  border-radius: 20px;
  backdrop-filter: blur(20px) saturate(180%);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.3) inset;
  border: none;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 24px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.section-title .el-icon {
  font-size: 20px;
  color: var(--el-color-primary);
}

.chart-section {
  margin-bottom: 32px;
}

.loading-container {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.chart-container-wrapper {
  position: relative;
  height: 300px;
}

.chart {
  width: 100%;
  height: 100%;
  min-height: 250px;
}

.progress-section {
  margin-bottom: 0;
}

.progress-grid {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

.progress-gauge-wrapper {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  flex: 0 0 150px;
}

.progress-gauge {
  width: 150px;
  height: 150px;
}

.progress-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.progress-bars {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.progress-value {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: var(--el-fill-color-blank);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--el-color-primary) 0%, var(--el-color-primary) 100%);
  border-radius: 4px;
  transition: width 0.3s ease;
}

/* Dark Mode */
html.dark .trends-section {
  background: rgba(30, 30, 30, 0.85);
}

html.dark .progress-bar {
  background: rgba(255, 255, 255, 0.05);
}

html.dark .progress-fill {
  background: linear-gradient(90deg, rgba(64, 158, 255, 0.3), rgba(64, 158, 255, 0.1));
}
</style>
