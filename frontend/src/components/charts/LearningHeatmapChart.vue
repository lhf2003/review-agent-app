<template>
  <div class="learning-heatmap-chart">
    <div v-if="loading" v-loading="true" class="chart-loading">
      <el-skeleton :rows="3" animated />
    </div>
    <div v-else-if="!hasData" class="chart-empty">
      <el-empty description="暂无学习记录" :image-size="120" />
    </div>
    <div v-else ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useThemeStore } from '../../stores/theme'

const props = defineProps({
  // 数据格式：{ '2026-01-01': 3, '2026-01-02': 1, ... }
  data: {
    type: Object,
    default: () => ({})
  },
  loading: {
    type: Boolean,
    default: false
  },
  // 图表高度
  height: {
    type: String,
    default: '200px'
  }
})

const emit = defineEmits(['chart-ready', 'chart-dispose', 'date-click'])

const themeStore = useThemeStore()
const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

const hasData = computed(() => props.data && Object.keys(props.data).length > 0)

// 生成日历数据
function generateCalendarData() {
  const now = new Date()
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const start = new Date(end.getFullYear() - 1, end.getMonth(), end.getDate() + 1)

  const dateList = []
  const valueList = []

  let current = new Date(start)
  while (current <= end) {
    const dateStr = formatDateToStr(current)
    dateList.push(dateStr)
    valueList.push(props.data[dateStr] || 0)
    current.setDate(current.getDate() + 1)
  }

  return { dateList, valueList }
}

function formatDateToStr(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// 初始化图表
function initChart() {
  if (!chartRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  const theme = themeStore.isDark ? 'dark' : undefined
  chartInstance = echarts.init(chartRef.value, theme, {
    backgroundColor: 'transparent',
    renderer: 'canvas'
  })

  // 停止之前的 ResizeObserver
  if (resizeObserver) {
    resizeObserver.disconnect()
  }

  // 监听容器尺寸变化
  resizeObserver = new ResizeObserver((entries) => {
    for (let entry of entries) {
      const { width, height } = entry.contentRect
      if (width > 0 && height > 0 && chartInstance) {
        chartInstance.resize()
      }
    }
  })

  resizeObserver.observe(chartRef.value)

  updateChart()
  emit('chart-ready', chartInstance)
}

// 更新图表
function updateChart() {
  // 如果图表未初始化，尝试初始化
  if (!chartInstance) {
    initChart()
    return
  }

  if (!hasData.value) return

  const { dateList, valueList } = generateCalendarData()
  const now = new Date()
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const start = new Date(end.getFullYear() - 1, end.getMonth(), end.getDate() + 1)

  // 计算最大值用于颜色映射
  const maxValue = Math.max(...valueList, 1)

  const option = {
    tooltip: {
      confine: true,
      backgroundColor: themeStore.isDark
        ? 'rgba(30, 30, 30, 0.9)'
        : 'rgba(255, 255, 255, 0.9)',
      borderColor: themeStore.isDark
        ? 'rgba(255, 255, 255, 0.1)'
        : 'rgba(0, 0, 0, 0.1)',
      textStyle: {
        color: themeStore.isDark ? '#fff' : '#333'
      },
      formatter: function (params) {
        if (params && params.value) {
          const count = params.value[1]
          return `${params.value[0]}<br/>完成测验: ${count} 次`
        }
        return ''
      }
    },
    visualMap: {
      min: 0,
      max: maxValue,
      calculable: false,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      itemWidth: 12,
      itemHeight: 80,
      inRange: {
        color: themeStore.isDark
          ? ['#2c2c2e', '#3d6b59', '#4ade80', '#22c55e', '#16a34a']
          : ['#ebedf0', '#9be9a8', '#40c463', '#30a14e', '#216e39']
      },
      text: ['多', '少'],
      textStyle: {
        color: themeStore.isDark ? '#909399' : '#606266',
        fontSize: 11
      }
    },
    calendar: {
      top: 40,
      left: 30,
      right: 30,
      bottom: 60,
      range: [start, end],
      cellSize: ['auto', 14],
      splitLine: {
        show: true,
        lineStyle: {
          color: themeStore.isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.08)'
        }
      },
      itemStyle: {
        color: themeStore.isDark ? '#2c2c2e' : '#ebedf0',
        borderWidth: 2,
        borderColor: themeStore.isDark ? '#1c1c1e' : '#fff',
        borderRadius: 2
      },
      yearLabel: {
        show: false
      },
      monthLabel: {
        show: true,
        nameMap: 'ZH',
        fontSize: 11,
        color: themeStore.isDark ? '#909399' : '#606266'
      },
      dayLabel: {
        show: true,
        nameMap: 'ZH',
        firstDay: 1,
        fontSize: 10,
        color: themeStore.isDark ? '#909399' : '#606266'
      }
    },
    series: [{
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: dateList.map((date, index) => [date, valueList[index]]),
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.3)'
        }
      }
    }]
  }

  chartInstance.setOption(option, true)

  // 点击事件
  chartInstance.off('click')
  chartInstance.on('click', function (params) {
    if (params && params.value) {
      emit('date-click', params.value[0])
    }
  })
}

// 调整图表大小
function resize() {
  if (!chartInstance) {
    initChart()
  } else {
    chartInstance.resize()
  }
}

// 监听数据变化
watch(() => props.data, () => {
  nextTick(() => {
    updateChart()
  })
}, { deep: true })

// 监听主题变化
watch(() => themeStore.isDark, () => {
  initChart()
})

onMounted(() => {
  requestAnimationFrame(() => {
    initChart()
    setTimeout(() => {
      resize()
    }, 100)
  })
})

onUnmounted(() => {
  // 停止 ResizeObserver
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }

  // 销毁图表实例
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
    emit('chart-dispose')
  }
})

// 暴露方法给父组件
defineExpose({
  resize,
  getInstance: () => chartInstance
})
</script>

<style scoped lang="scss">
.learning-heatmap-chart {
  width: 100%;
  height: 100%;
  min-height: 180px;
}

.chart-loading,
.chart-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
}

.chart-container {
  width: 100%;
  height: v-bind(height);
  min-height: 180px;
}
</style>
