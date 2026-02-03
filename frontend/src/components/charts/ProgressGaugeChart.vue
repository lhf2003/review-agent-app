<template>
  <div class="progress-gauge-chart">
    <div v-if="loading" v-loading="true" class="chart-loading">
      <el-skeleton :rows="1" animated />
    </div>
    <div v-else ref="chartRef" class="chart-container"></div>
    <div v-if="!loading" class="chart-label">{{ label }}</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useThemeStore } from '../../stores/theme'

const props = defineProps({
  // 进度值 0-100
  value: {
    type: Number,
    default: 0,
    validator: (value) => value >= 0 && value <= 100
  },
  loading: {
    type: Boolean,
    default: false
  },
  // 图表尺寸
  size: {
    type: String,
    default: '160px'
  },
  // 标签文字
  label: {
    type: String,
    default: '总体进度'
  },
  // 仪表盘起始角度
  startAngle: {
    type: Number,
    default: 180
  },
  // 仪表盘结束角度
  endAngle: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['chart-ready', 'chart-dispose'])

const themeStore = useThemeStore()
const chartRef = ref(null)
let chartInstance = null

// 颜色映射函数
function getColor(value) {
  if (value >= 80) return '#67C23A' // 绿色
  if (value >= 60) return '#409EFF' // 蓝色
  if (value >= 40) return '#E6A23C' // 橙色
  return '#F56C6C' // 红色
}

// 初始化图表
function initChart() {
  if (!chartRef.value) return

  // 如果容器不可见（宽高为0），则不初始化
  if (chartRef.value.clientWidth === 0 || chartRef.value.clientHeight === 0) {
    return
  }

  // 销毁旧实例
  if (chartInstance) {
    chartInstance.dispose()
  }

  const theme = themeStore.isDark ? 'dark' : undefined
  chartInstance = echarts.init(chartRef.value, theme, {
    backgroundColor: 'transparent',
    renderer: 'canvas'
  })

  updateChart()
  emit('chart-ready', chartInstance)
}

// 更新图表
function updateChart() {
  if (!chartInstance) return

  const progress = props.value
  const color = getColor(progress)

  const option = {
    tooltip: {
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
        return props.label + ': ' + params.value + '%'
      }
    },
    series: [{
      type: 'gauge',
      startAngle: props.startAngle,
      endAngle: props.endAngle,
      min: 0,
      max: 100,
      splitNumber: 10,
      itemStyle: {
        color: color
      },
      progress: {
        show: true,
        width: 18
      },
      pointer: {
        length: '60%',
        width: 6,
        itemStyle: {
          color: color
        }
      },
      axisLine: {
        lineStyle: {
          width: 18,
          color: [[1, themeStore.isDark ? 'rgba(255,255,255,0.15)' : 'rgba(0,0,0,0.1)']]
        }
      },
      axisTick: {
        distance: -20,
        length: 8,
        lineStyle: {
          color: themeStore.isDark ? 'rgba(255,255,255,0.3)' : 'rgba(0,0,0,0.3)',
          width: 2
        }
      },
      splitLine: {
        distance: -24,
        length: 12,
        lineStyle: {
          color: themeStore.isDark ? 'rgba(255,255,255,0.3)' : 'rgba(0,0,0,0.3)',
          width: 2
        }
      },
      axisLabel: {
        color: themeStore.isDark ? '#909399' : '#606266',
        fontSize: 11,
        distance: -35,
        formatter: function (value) {
          return value + '%'
        }
      },
      detail: {
        valueAnimation: true,
        formatter: function (value) {
          return Math.round(value) + '%'
        },
        fontSize: 28,
        offsetCenter: [0, '10%'],
        valueStyle: {
          color: themeStore.isDark ? '#fff' : '#333',
          fontSize: 32,
          fontWeight: 600
        }
      },
      data: [{
        value: progress,
        name: ''
      }]
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
watch(() => props.value, () => {
  nextTick(() => {
    updateChart()
  })
})

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
.progress-gauge-chart {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.chart-loading {
  width: v-bind(size);
  height: v-bind(size);
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-container {
  width: v-bind(size);
  height: v-bind(size);
}

.chart-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
  text-align: center;
}
</style>
