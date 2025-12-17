<template>
  <div class="page">
    <!-- Word Cloud Section -->
    <div class="section">
      <div class="header">
        <h2>词云</h2>
        <el-button type="primary" @click="loadWordCloud" :loading="wordCloudLoading">刷新</el-button>
      </div>
      <div v-loading="wordCloudLoading" class="chart-container">
        <div ref="wordCloudChartRef" class="chart"></div>
      </div>
    </div>

    <!-- Trend Chart Section -->
    <div class="section">
      <div class="header">
        <h2>标签趋势</h2>
        <div class="controls">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            :shortcuts="shortcuts"
            @change="loadTrend"
            style="width: 300px"
          />
          <el-button type="primary" @click="loadTrend" :loading="trendLoading">刷新</el-button>
        </div>
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

async function loadWordCloud() {
  wordCloudLoading.value = true
  try {
    const userId = auth.userId || null
    const data = await api.getWordReport(userId)
    wordCloudSource.value = data || {}
    updateWordCloudChart()
  } finally {
    wordCloudLoading.value = false
  }
}

async function loadTrend() {
  if (!dateRange.value || dateRange.value.length !== 2) return
  trendLoading.value = true
  try {
    const startDate = formatDate(dateRange.value[0])
    // Backend treats endDate as exclusive, so add 1 day to include the selected end date
    const endDateObj = new Date(dateRange.value[1])
    endDateObj.setDate(endDateObj.getDate() + 1)
    const endDate = formatDate(endDateObj)
    const res = await api.getDateTagCountTrend(startDate, endDate)
    trendSource.value = res || {}
    updateTrendChart()
  } catch (e) {
    console.error(e)
  } finally {
    trendLoading.value = false
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

function updateTrendChart() {
  if (!trendChartInstance) return

  const dataMap = trendSource.value || {}
  // dataMap is { "2025-12-10": [ {tagName, count}, ... ], ... }
  
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
      bottom: '10%', // Make room for legend
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

  trendChartInstance.setOption(option, true) // true to merge=false (clear previous)
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
  loadWordCloud()
  loadTrend()
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
  gap: 24px;
  padding: 16px;
  height: 100%;
  overflow-y: auto;
}
.section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--el-bg-color-overlay);
  padding: 16px;
  border-radius: 8px;
  box-shadow: var(--el-box-shadow-light);
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.controls {
  display: flex;
  gap: 12px;
  align-items: center;
}
.chart-container {
  width: 100%;
  height: 400px;
  position: relative;
}
.chart {
  width: 100%;
  height: 100%;
}
</style>
