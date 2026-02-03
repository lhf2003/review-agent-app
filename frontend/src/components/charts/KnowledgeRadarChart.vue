<template>
  <div class="knowledge-radar-chart">
    <div v-if="loading" v-loading="true" class="chart-loading">
      <el-skeleton :rows="1" animated />
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
  // 数据格式：[{ tagName: 'Vue.js', accuracyRate: 85.5 }, ...]
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
  },
  // 最大值（雷达图维度最大值）
  max: {
    type: Number,
    default: 100
  }
})

const emit = defineEmits(['chart-ready', 'chart-dispose'])

const themeStore = useThemeStore()
const chartRef = ref(null)
let chartInstance = null

const hasData = computed(() => props.data && props.data.length > 0)

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
  if (!chartInstance || !hasData.value) return

  const indicators = props.data.map(item => item.tagName)
  const accuracyData = props.data.map(item => (item.accuracyRate || 0).toFixed(1))

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
        return params[0].name + '<br/>正确率: ' + params[0].value + '%'
      }
    },
    legend: {
      data: ['正确率'],
      bottom: 0,
      textStyle: {
        color: themeStore.isDark ? '#909399' : '#606266'
      }
    },
    radar: {
      indicator: indicators.map(name => ({
        name: name,
        max: props.max
      })),
      splitArea: {
        areaStyle: {
          color: themeStore.isDark
            ? ['rgba(255,255,255,0.1)', 'rgba(255,255,255,0.05)']
            : ['rgba(0,0,0,0.05)', 'rgba(0,0,0,0.02)']
        }
      },
      axisLine: {
        lineStyle: {
          color: themeStore.isDark ? 'rgba(255,255,255,0.3)' : 'rgba(0,0,0,0.3)'
        }
      },
      splitLine: {
        lineStyle: {
          color: themeStore.isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)'
        }
      },
      axisName: {
        color: themeStore.isDark ? '#909399' : '#606266',
        fontSize: 12
      }
    },
    series: [{
      name: '知识点掌握度',
      type: 'radar',
      data: [{
        value: accuracyData,
        name: '正确率'
      }],
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: {
        color: '#67C23A',
        width: 2
      },
      itemStyle: {
        color: '#67C23A',
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
            color: 'rgba(103, 194, 58, 0.3)'
          }, {
            offset: 1,
            color: 'rgba(103, 194, 58, 0.1)'
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
.knowledge-radar-chart {
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
