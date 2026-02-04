<script setup>
import { onMounted, onUnmounted, ref, nextTick, watch } from 'vue'
import { FullScreen, Close } from '@element-plus/icons-vue'
import { useThemeStore } from '../../stores/theme'
import { api } from '../../api/http'
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import 'echarts/theme/dark'

const props = defineProps({
  dateRange: {
    type: Array,
    required: true
  },
  embedded: {
    type: Boolean,
    default: false
  },
  showHeader: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['fullscreenToggle'])

const themeStore = useThemeStore()

// Word Cloud State
const wordCloudSource = ref({})
const wordCloudChartRef = ref(null)
let wordCloudChartInstance = null

// Fullscreen State
const isFullscreen = ref(false)

defineExpose({
  toggleFullscreen,
  resize: handleResize
})

function formatDate(date) {
  if (!date) return null
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function getFormattedDateRange() {
  if (!props.dateRange || props.dateRange.length !== 2) return null
  const startDate = formatDate(props.dateRange[0])
  // Backend treats endDate as exclusive, so add 1 day to include the selected end date
  const endDateObj = new Date(props.dateRange[1])
  endDateObj.setDate(endDateObj.getDate() + 1)
  const endDate = formatDate(endDateObj)
  return { startDate, endDate }
}

async function loadWordCloud() {
  const dates = getFormattedDateRange()
  if (!dates) return

  try {
    const data = await api.getWordReport(dates.startDate, dates.endDate)
    wordCloudSource.value = data || {}
    updateWordCloudChart()
  } catch (e) {
    console.error(e)
  }
}

function updateWordCloudChart() {
  if (!wordCloudChartInstance) return

  const entries = Object.entries(wordCloudSource.value || {})
  const data = entries.map(([name, value]) => ({
    name,
    value: Number(value) || 0
  }))

  const option = {
    tooltip: { show: true },
    series: [{
      type: 'wordCloud',
      shape: 'circle',
      left: 'center',
      top: 'center',
      width: '100%',
      height: '100%',
      sizeRange: [14, 60],
      rotationRange: [0, 0],
      gridSize: 8,
      drawOutOfBound: false,
      layoutAnimation: true,
      textStyle: {
        fontFamily: 'sans-serif',
        fontWeight: 'bold',
        color: function () {
          if (themeStore.isDark) {
            return 'rgb(' + [
              Math.round(100 + Math.random() * 155),
              Math.round(100 + Math.random() * 155),
              Math.round(100 + Math.random() * 155)
            ].join(',') + ')'
          } else {
            return 'rgb(' + [
              Math.round(Math.random() * 160),
              Math.round(Math.random() * 160),
              Math.round(Math.random() * 160)
            ].join(',') + ')'
          }
        }
      },
      emphasis: {
        focus: 'self',
        textStyle: { textShadowBlur: 0 }
      },
      data: data
    }]
  }
  wordCloudChartInstance.setOption(option)
}

function initChart() {
  const theme = themeStore.isDark ? 'dark' : undefined
  if (wordCloudChartRef.value) {
    wordCloudChartInstance = echarts.init(wordCloudChartRef.value, theme, { backgroundColor: 'transparent' })
  }
}

function handleResize() {
  wordCloudChartInstance?.resize()
}

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  emit('fullscreenToggle', isFullscreen.value)
  nextTick(() => {
    wordCloudChartInstance?.resize()
  })
}

// Watch dateRange changes
watch(() => props.dateRange, () => {
  loadWordCloud()
}, { deep: true })

// Watch theme changes
watch(() => themeStore.isDark, () => {
  wordCloudChartInstance?.dispose()
  initChart()
  updateWordCloudChart()
})

onMounted(() => {
  initChart()
  loadWordCloud()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  wordCloudChartInstance?.dispose()
})
</script>

<template>
  <div class="wordcloud-chart" :class="{ 'is-fullscreen': isFullscreen }">
    <div class="chart-header" v-if="showHeader">
      <h3>标签词云</h3>
      <el-button link @click="toggleFullscreen" v-if="embedded">
        <el-icon><FullScreen /></el-icon>
      </el-button>
    </div>
    
    <!-- Fullscreen Exit Button -->
    <div v-if="isFullscreen" class="fullscreen-exit-btn">
      <el-button circle @click="toggleFullscreen">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <div class="chart-container">
      <div ref="wordCloudChartRef" class="chart"></div>
    </div>
  </div>
</template>

<style scoped>
.wordcloud-chart {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  flex: 1;
  min-height: 0;
}

.wordcloud-chart.is-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 2000;
  border-radius: 0;
  background: var(--el-bg-color-page);
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}

.chart-header h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--el-text-color-primary);
}

.chart-container {
  flex: 1;
  width: 100%;
  min-height: 300px;
  position: relative;
}

.chart {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.fullscreen-exit-btn {
  position: absolute;
  top: 24px;
  right: 24px;
  z-index: 2100;
}

.fullscreen-exit-btn .el-button {
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: var(--el-text-color-primary);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

html.dark .fullscreen-exit-btn .el-button {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
}
</style>
