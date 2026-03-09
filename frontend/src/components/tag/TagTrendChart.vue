<script setup>
import { onMounted, onUnmounted, ref, nextTick, watch } from 'vue'
import { FullScreen, Close } from '@element-plus/icons-vue'
import { api } from '../../api/http'
import * as echarts from 'echarts'
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

// Trend Chart State
const trendSource = ref({})
const trendLoading = ref(false)
const trendChartRef = ref(null)
let trendChartInstance = null

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

async function loadTrend() {
  const dates = getFormattedDateRange()
  if (!dates) return

  trendLoading.value = true
  try {
    const res = await api.getDateTagCountTrend(dates.startDate, dates.endDate)
    trendSource.value = res || {}
    updateTrendChart()
  } catch {
  } finally {
    trendLoading.value = false
  }
}

function updateTrendChart() {
  if (!trendChartInstance) return

  const dataMap = trendSource.value || {}

  const dates = Object.keys(dataMap).sort()

  // Extract all unique tag names
  const allTags = new Set()
  dates.forEach(date => {
    const list = dataMap[date] || []
    list.forEach(item => allTags.add(item.tagName))
  })
  const tagList = Array.from(allTags)

  // Build series
  const series = tagList.map(tag => {
    const data = dates.map(date => {
      const list = dataMap[date] || []
      const item = list.find(i => i.tagName === tag)
      return item ? item.count : 0
    })
    return {
      name: tag,
      type: 'line',
      smooth: true,
      data: data
    }
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      confine: true,
      formatter: function (params) {
        let result = params[0].axisValueLabel + '<br/>'
        let hasData = false
        params.forEach(item => {
          if (item.value > 0) {
            hasData = true
            result += item.marker + ' ' + item.seriesName + ': ' + item.value + '<br/>'
          }
        })
        return hasData ? result : ''
      }
    },
    legend: {
      data: tagList,
      bottom: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates
    },
    yAxis: {
      type: 'value'
    },
    series: series
  }

  trendChartInstance.setOption(option, true)
}

function initChart() {
  if (trendChartRef.value) {
    trendChartInstance = echarts.init(trendChartRef.value, 'dark', { backgroundColor: 'transparent' })
  }
}

function handleResize() {
  trendChartInstance?.resize()
}

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  emit('fullscreenToggle', isFullscreen.value)
  nextTick(() => {
    trendChartInstance?.resize()
  })
}

// Watch dateRange changes
watch(() => props.dateRange, () => {
  loadTrend()
}, { deep: true })

onMounted(() => {
  initChart()
  loadTrend()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChartInstance?.dispose()
})
</script>

<template>
  <div class="trend-chart" :class="{ 'is-fullscreen': isFullscreen }">
    <div class="chart-header" v-if="showHeader">
      <h3>标签趋势</h3>
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

    <div v-loading="trendLoading" class="chart-container">
      <div ref="trendChartRef" class="chart"></div>
    </div>
  </div>
</template>

<style scoped>
.trend-chart {
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

.trend-chart.is-fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 2000;
  border-radius: 0;
  background: var(--bg-deep);
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--glass-border);
  flex-shrink: 0;
}

.chart-header h3 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
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
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
</style>
