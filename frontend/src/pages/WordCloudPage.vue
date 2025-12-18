<template>
  <div class="page">
    <!-- Global Controls -->
    <div class="global-controls">
      <div class="date-picker-wrapper">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :shortcuts="shortcuts"
          @change="handleDateChange"
          style="width: 100%"
        />
      </div>
      <el-button type="primary" @click="handleRefresh" :loading="wordCloudLoading || trendLoading">刷新</el-button>
    </div>

    <!-- Word Cloud Section -->
    <div class="section">
      <div class="header">
        <h2>词云</h2>
      </div>
      <div v-loading="wordCloudLoading" class="chart-container">
        <div ref="wordCloudChartRef" class="chart"></div>
      </div>
    </div>

    <!-- Trend Chart Section -->
    <div class="section">
      <div class="header">
        <h2>标签趋势</h2>
      </div>
      <div v-loading="trendLoading" class="chart-container">
        <div ref="trendChartRef" class="chart"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, nextTick, watch } from 'vue'
import { useAuthStore } from '../stores/auth'
import { useThemeStore } from '../stores/theme'
import { api } from '../api/http'
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import 'echarts/theme/dark'

const auth = useAuthStore()
const themeStore = useThemeStore()

// Word Cloud State
const wordCloudSource = ref({})
const wordCloudLoading = ref(false)
const wordCloudChartRef = ref(null)
let wordCloudChartInstance = null

// Trend Chart State
const trendSource = ref({})
const trendLoading = ref(false)
const trendChartRef = ref(null)
let trendChartInstance = null

const end = new Date()
const start = new Date()
start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
const dateRange = ref([start, end])

const shortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    },
  },
  {
    text: '最近一个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    },
  },
  {
    text: '最近三个月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
      return [start, end]
    },
  },
]

function formatDate(date) {
  if (!date) return null
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function getFormattedDateRange() {
    if (!dateRange.value || dateRange.value.length !== 2) return null
    const startDate = formatDate(dateRange.value[0])
    // Backend treats endDate as exclusive, so add 1 day to include the selected end date
    const endDateObj = new Date(dateRange.value[1])
    endDateObj.setDate(endDateObj.getDate() + 1)
    const endDate = formatDate(endDateObj)
    return { startDate, endDate }
}

async function loadWordCloud() {
  const dates = getFormattedDateRange()
  if (!dates) return

  wordCloudLoading.value = true
  try {
    const userId = auth.userId || null
    // Pass dates to API
    const data = await api.getWordReport(dates.startDate, dates.endDate)
    wordCloudSource.value = data || {}
    updateWordCloudChart()
  } finally {
    wordCloudLoading.value = false
  }
}

async function loadTrend() {
  const dates = getFormattedDateRange()
  if (!dates) return

  trendLoading.value = true
  try {
    const res = await api.getDateTagCountTrend(dates.startDate, dates.endDate)
    trendSource.value = res || {}
    updateTrendChart()
  } catch (e) {
    console.error(e)
  } finally {
    trendLoading.value = false
  }
}

function handleRefresh() {
    loadWordCloud()
    loadTrend()
}

function handleDateChange() {
    handleRefresh()
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

function initCharts() {
  const theme = themeStore.isDark ? 'dark' : undefined
  if (wordCloudChartRef.value) {
    wordCloudChartInstance = echarts.init(wordCloudChartRef.value, theme, { backgroundColor: 'transparent' })
  }
  if (trendChartRef.value) {
    trendChartInstance = echarts.init(trendChartRef.value, theme, { backgroundColor: 'transparent' })
  }
}

function handleResize() {
  wordCloudChartInstance?.resize()
  trendChartInstance?.resize()
}

watch(() => themeStore.isDark, () => {
  wordCloudChartInstance?.dispose()
  trendChartInstance?.dispose()
  initCharts()
  updateWordCloudChart()
  updateTrendChart()
})

onMounted(() => {
  initCharts()
  handleRefresh()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  wordCloudChartInstance?.dispose()
  trendChartInstance?.dispose()
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  height: 100%;
  overflow-y: auto;
}
.global-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--el-bg-color-overlay);
  padding: 12px 16px;
  border-radius: 6px;
  box-shadow: var(--el-box-shadow-light);
}
.date-picker-wrapper {
  flex: 1;
  max-width: 400px;
}
.section {
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color-overlay);
  padding: 0;
  border-radius: 6px;
  box-shadow: var(--el-box-shadow-light);
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
}
.header h2 {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
  color: var(--el-text-color-primary);
}
.chart-container {
  width: 100%;
  height: 100%;
  min-height: 200px;
  padding: 0px;
  position: relative;
  flex: 1;
  box-sizing: border-box;
}
.chart {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
</style>