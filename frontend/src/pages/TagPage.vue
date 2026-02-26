<script setup>
import { ref } from 'vue'
import { FullScreen } from '@element-plus/icons-vue'
import TagManagementPane from '../components/tag/TagManagementPane.vue'
import WordCloudChart from '../components/tag/WordCloudChart.vue'
import TagTrendChart from '../components/tag/TagTrendChart.vue'
import DateRangeFilter from '../components/tag/DateRangeFilter.vue'

const activeView = ref('management') // 'management', 'wordcloud', 'trends'

// Date range for charts
const end = new Date()
const start = new Date()
start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
const dateRange = ref([start, end])

const wordCloudRef = ref(null)
const trendChartRef = ref(null)

function handleDateChange() {
  // 子组件通过 watch 自动响应 dateRange 变化
}

function toggleWordCloudFullscreen() {
  wordCloudRef.value?.toggleFullscreen()
}

function toggleTrendFullscreen() {
  trendChartRef.value?.toggleFullscreen()
}
</script>

<template>
  <div class="tag-page-root">
    <!-- 顶部 Tab 导航 -->
    <div class="top-nav-container">
      <el-radio-group v-model="activeView" class="nav-radio-group">
        <el-radio-button value="management">标签管理</el-radio-button>
        <el-radio-button value="wordcloud">词云分析</el-radio-button>
        <el-radio-button value="trends">标签趋势</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 主内容区 -->
    <main class="content-area">
      <Transition name="fade" mode="out-in">
        <!-- 标签管理视图 -->
        <div v-if="activeView === 'management'" key="management" class="view-container">
          <TagManagementPane :embedded="true" />
        </div>

        <!-- 词云分析视图 -->
        <div v-else-if="activeView === 'wordcloud'" key="wordcloud" class="view-container chart-view">
          <div class="chart-wrapper glass-panel">
            <div class="chart-header">
              <div class="header-title">
                <h2>标签词云分析</h2>
                <p class="subtitle">查看标签使用频率分布</p>
              </div>
              <div class="header-actions">
                <DateRangeFilter v-model="dateRange" @change="handleDateChange" />
                <el-button class="fullscreen-btn" text circle @click="toggleWordCloudFullscreen">
                  <el-icon><FullScreen /></el-icon>
                </el-button>
              </div>
            </div>
            <WordCloudChart 
              ref="wordCloudRef" 
              :date-range="dateRange" 
              :embedded="true" 
              :show-header="false" 
            />
          </div>
        </div>

        <!-- 标签趋势视图 -->
        <div v-else-if="activeView === 'trends'" key="trends" class="view-container chart-view">
          <div class="chart-wrapper glass-panel">
            <div class="chart-header">
              <div class="header-title">
                <h2>标签趋势统计</h2>
                <p class="subtitle">追踪标签使用变化趋势</p>
              </div>
              <div class="header-actions">
                <DateRangeFilter v-model="dateRange" @change="handleDateChange" />
                <el-button class="fullscreen-btn" text circle @click="toggleTrendFullscreen">
                  <el-icon><FullScreen /></el-icon>
                </el-button>
              </div>
            </div>
            <TagTrendChart 
              ref="trendChartRef" 
              :date-range="dateRange" 
              :embedded="true" 
              :show-header="false" 
            />
          </div>
        </div>
      </Transition>
    </main>
  </div>
</template>

<style scoped>
.tag-page-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden;
  gap: 16px;
  padding: 0 6px;
  min-height: 0;
}

/* ============ Top Nav ============ */
.top-nav-container {
  padding: 4px 0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 10;
}

.nav-radio-group {
  --el-fill-color-light: rgba(255, 255, 255, 0.5);
  --el-border-radius-base: 8px;
}

.nav-radio-group :deep(.el-radio-button__inner) {
  border: none;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  border-radius: 8px;
  padding: 6px 12px;
  min-width: 90px;
  font-size: 14px;
}

.nav-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: var(--el-color-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.3);
}

/* ============ Content Area ============ */
.content-area {
  flex: 1;
  padding: 0;
  overflow: hidden;
  position: relative;
  min-height: 0;
}

.view-container {
  height: 100%;
  width: 100%;
}

.chart-view {
  display: flex;
  flex-direction: column;
}

/* ============ Glassmorphism ============ */
.glass-panel {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 4px 24px -1px rgba(0, 0, 0, 0.06),
    0 0 0 1px rgba(255, 255, 255, 0.4) inset;
  border-radius: 16px;
  padding: 20px;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ============ Chart Header ============ */
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}

.header-title h2 {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 4px 0;
  color: var(--el-text-color-primary);
}

.header-title .subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.fullscreen-btn {
  color: var(--el-text-color-secondary);
  transition: all 0.3s ease;
}

.fullscreen-btn:hover {
  color: var(--el-color-primary);
  background: var(--el-fill-color-light);
  transform: scale(1.1);
}

.chart-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* ============ Transitions ============ */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ============ Dark Mode ============ */
html.dark .glass-panel {
  background: rgba(28, 28, 30, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
}

html.dark .nav-radio-group :deep(.el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.1);
  color: var(--el-text-color-regular);
}

html.dark .nav-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: var(--el-color-primary);
  color: white;
}
</style>
