<template>
  <div class="time-distribution-chart">
    <div v-if="loading" v-loading="true" class="chart-loading">
      <el-skeleton :rows="2" animated />
    </div>
    <div v-else-if="!hasData" class="chart-empty">
      <el-empty description="暂无数据" :image-size="100" />
    </div>
    <div v-else ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  // 数据格式：{ '凌晨': 5, '上午': 15, '下午': 25, '晚上': 30 }
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
    default: '280px'
  }
})

const emit = defineEmits(['chart-ready', 'chart-dispose'])

const chartRef = ref(null)
let chartInstance = null
let resizeObserver = null

// 时段顺序和图标
const timeSlots = ['凌晨', '上午', '下午', '晚上']
const timeColors = ['#909399', '#E6A23C', '#409EFF', '#67C23A']
const timeIcons = ['moon', 'sunrise', 'sun', 'sunset']

const hasData = computed(() => props.data && Object.keys(props.data).length > 0)

// 初始化图表
function initChart() {
  if (!chartRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value, 'dark', {
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

  // 按照固定顺序组织数据
  const chartData = timeSlots.map((slot, index) => ({
    name: slot,
    value: props.data[slot] || 0,
    itemStyle: {
      color: timeColors[index]
    }
  }))

  // 计算总数
  const total = chartData.reduce((sum, item) => sum + item.value, 0)

  const option = {
    tooltip: {
      trigger: 'item',
      confine: true,
      backgroundColor: 'rgba(30, 30, 30, 0.9)',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      textStyle: {
        color: '#fff'
      },
      formatter: function (params) {
        const percent = total > 0 ? ((params.value / total) * 100).toFixed(1) : 0
        return `${params.name}<br/>${params.value} 次 (${percent}%)`
      }
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center',
      textStyle: {
        color: '#909399',
        fontSize: 13
      },
      itemWidth: 12,
      itemHeight: 12,
      itemGap: 16
    },
    graphic: [{
      type: 'text',
      left: '28%',
      top: '45%',
      style: {
        text: total,
        textAlign: 'center',
        fill: '#fff',
        fontSize: 28,
        fontWeight: 'bold'
      }
    }, {
      type: 'text',
      left: '28%',
      top: '58%',
      style: {
        text: '总测验',
        textAlign: 'center',
        fill: '#909399',
        fontSize: 12
      }
    }],
    series: [{
      name: '学习时段',
      type: 'pie',
      radius: ['50%', '70%'],
      center: ['30%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#1c1c1e',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        scale: true,
        scaleSize: 8,
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.3)'
        }
      },
      labelLine: {
        show: false
      },
      data: chartData
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
.time-distribution-chart {
  width: 100%;
  height: 100%;
  min-height: 220px;
}

.chart-loading,
.chart-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 220px;
}

.chart-container {
  width: 100%;
  height: v-bind(height);
  min-height: 220px;
}
</style>
