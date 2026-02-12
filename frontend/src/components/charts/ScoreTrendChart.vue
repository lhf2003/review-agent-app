<template>
  <div class="score-trend-chart">
    <div v-if="loading" v-loading="true" class="chart-loading">
      <el-skeleton :rows="3" animated />
    </div>
    <div v-else-if="!hasData" class="chart-empty">
      <el-empty description="暂无数据" :image-size="120" />
    </div>
    <div v-else ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useThemeStore } from '../../stores/theme'

const props = defineProps({
  // 数据格式：[{ date: '2026-02-01', score: 85 }, ...]
  data: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  // 图表高度
  height: {
    type: String,
    default: '300px'
  }
})

const emit = defineEmits(['chart-ready', 'chart-dispose'])

const themeStore = useThemeStore()
const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

const hasData = computed(() => props.data && props.data.length > 0)

// 初始化图表
function initChart() {
  if (!chartRef.value) return

  // 销毁旧实例
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

  const dates = props.data.map(item => item.date)
  const scores = props.data.map(item => item.score)

  const option = {
    tooltip: {
      trigger: 'axis',
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
        if (params && params[0]) {
          return `${params[0].axisValueLabel}<br/>${params[0].marker} ${params[0].seriesName}: ${params[0].value}分`
        }
        return ''
      }
    },
    legend: {
      data: ['测验分数'],
      bottom: 0,
      textStyle: {
        color: themeStore.isDark ? '#909399' : '#606266'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLabel: {
        color: themeStore.isDark ? '#909399' : '#606266',
        fontSize: 12
      },
      axisLine: {
        lineStyle: {
          color: themeStore.isDark ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.2)'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '分数',
      nameTextStyle: {
        color: themeStore.isDark ? '#909399' : '#606266'
      },
      axisLabel: {
        color: themeStore.isDark ? '#909399' : '#606266',
        fontSize: 12
      },
      axisLine: {
        lineStyle: {
          color: themeStore.isDark ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.2)'
        }
      },
      splitLine: {
        lineStyle: {
          color: themeStore.isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)'
        }
      }
    },
    series: [{
      name: '测验分数',
      type: 'line',
      smooth: true,
      data: scores,
      lineStyle: {
        color: '#409EFF',
        width: 3
      },
      itemStyle: {
        color: '#409EFF',
        borderWidth: 2,
        borderColor: '#fff'
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [{
            offset: 0,
            color: 'rgba(64, 158, 255, 0.3)'
          }, {
            offset: 1,
            color: 'rgba(64, 158, 255, 0.05)'
          }]
        }
      }
    }]
  }

  chartInstance.setOption(option, true)
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
  // 使用 requestAnimationFrame 确保 DOM 渲染完成
  requestAnimationFrame(() => {
    initChart()
    // 再次延迟 resize 确保父容器也渲染完成
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
.score-trend-chart {
  width: 100%;
  height: 100%;
  min-height: 250px;
}

.chart-loading,
.chart-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 250px;
}

.chart-container {
  width: 100%;
  height: v-bind(height);
  min-height: 250px;
}
</style>
