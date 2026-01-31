import { ref } from 'vue'
import { api } from '../../../api/http'
import * as echarts from 'echarts'
import { useThemeStore } from '../../../stores/theme'

// 学习成就相关状态
export function useAchievements() {
  const themeStore = useThemeStore()

  // 图表 refs
  const scoreTrendChartRef = ref(null)
  const masteryRadarChartRef = ref(null)
  const progressGaugeChartRef = ref(null)

  // 图表实例
  let scoreTrendChartInstance = null
  let masteryRadarChartInstance = null
  let progressGaugeChartInstance = null

  // 加载状态
  const loading = ref({
    achievements: false,
    trends: false,
    progress: false
  })

  // 学习成就数据
  const achievementsData = ref({
    achievements: [],
    quizScoreTrend: [],
    knowledgeMastery: [],
    learningProgress: {
      overallProgress: 0,
      syncProgress: 0,
      analysisProgress: 0,
      quizProgress: 0,
      achievementProgress: 0
    }
  })

  // 加载学习成就数据
  async function loadAchievementsData() {
    loading.value.achievements = true
    try {
      const resp = await api.getUserStats()
      const data = resp?.data || resp

      achievementsData.value = {
        achievements: data.achievements || [],
        quizScoreTrend: data.quizScoreTrend || [],
        knowledgeMastery: data.knowledgeMastery || [],
        learningProgress: data.learningProgress || {
          overallProgress: 0,
          syncProgress: 0,
          analysisProgress: 0,
          quizProgress: 0,
          achievementProgress: 0
        }
      }

      // 更新图表
      updateCharts()
    } catch (e) {
      console.error('加载学习成就数据失败:', e)
    } finally {
      loading.value.achievements = false
    }
  }

  // 初始化图表
  function initCharts() {
    const theme = themeStore.isDark ? 'dark' : undefined
    if (scoreTrendChartRef.value) {
      scoreTrendChartInstance = echarts.init(scoreTrendChartRef.value, theme, { backgroundColor: 'transparent' })
    }
    if (masteryRadarChartRef.value) {
      masteryRadarChartInstance = echarts.init(masteryRadarChartRef.value, theme, { backgroundColor: 'transparent' })
    }
    if (progressGaugeChartRef.value) {
      progressGaugeChartInstance = echarts.init(progressGaugeChartRef.value, theme, { backgroundColor: 'transparent' })
    }
  }

  // 更新图表
  function updateCharts() {
    updateScoreTrendChart()
    updateMasteryRadarChart()
    updateProgressGaugeChart()
  }

  // 处理窗口大小变化
  function handleResize() {
    scoreTrendChartInstance?.resize()
    masteryRadarChartInstance?.resize()
    progressGaugeChartInstance?.resize()
  }

  // 更新测验分数趋势图
  function updateScoreTrendChart() {
    if (!scoreTrendChartInstance || !achievementsData.value.quizScoreTrend || achievementsData.value.quizScoreTrend.length === 0) return

    const dates = achievementsData.value.quizScoreTrend.map(item => item.date)
    const scores = achievementsData.value.quizScoreTrend.map(item => item.score)

    const option = {
      tooltip: {
        trigger: 'axis',
        confine: true,
        formatter: function (params) {
          if (params && params[0]) {
            return `${params[0].axisValueLabel}<br/>${params[0].marker} ${params[0].seriesName}: ${params[0].value}分`
          }
          return ''
        }
      },
      legend: {
        data: ['测验分数'],
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
        data: dates,
        axisLabel: {
          color: themeStore.isDark ? '#909399' : '#606266'
        }
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          color: themeStore.isDark ? '#909399' : '#606266'
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
          borderColor: '#fff',
          borderWidth: themeStore.isDark ? 0 : 2
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
              color: 'rgba(64, 158, 255, 0.1)'
            }]
          }
        }
      }]
    }

    scoreTrendChartInstance.setOption(option)
  }

  // 更新知识点掌握度雷达图
  function updateMasteryRadarChart() {
    if (!masteryRadarChartInstance || !achievementsData.value.knowledgeMastery || achievementsData.value.knowledgeMastery.length === 0) return

    const indicators = achievementsData.value.knowledgeMastery.map(item => item.tagName)
    const accuracyData = achievementsData.value.knowledgeMastery.map(item => (item.accuracyRate || 0).toFixed(1))

    const option = {
      tooltip: {
        confine: true,
        formatter: function (params) {
          return params[0].name + '<br/>正确率: ' + params[0].value + '%'
        }
      },
      legend: {
        data: ['正确率'],
        bottom: 0
      },
      radar: {
        indicator: indicators.map(name => ({
          name: name,
          max: 100
        })),
        splitArea: {
          areaStyle: {
            color: themeStore.isDark ? ['rgba(255,255,255,0.1)', 'rgba(255,255,255,0.05)'] : ['rgba(0,0,0,0.1)', 'rgba(0,0,0,0.05)']
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
        }
      },
      series: [{
        name: '知识点掌握度',
        type: 'radar',
        data: accuracyData,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: {
          color: '#67C23A',
          width: 2
        },
        itemStyle: {
          color: '#67C23A',
          borderWidth: 2,
          borderColor: '#fff',
          borderWidth: themeStore.isDark ? 0 : 2
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

    masteryRadarChartInstance.setOption(option)
  }

  // 更新学习进度环形图
  function updateProgressGaugeChart() {
    if (!progressGaugeChartInstance || !achievementsData.value.learningProgress) return

    const progress = achievementsData.value.learningProgress.overallProgress || 0

    const option = {
      tooltip: {
        formatter: function (params) {
          return '总体进度: ' + params.value + '%'
        }
      },
      series: [{
        type: 'gauge',
        startAngle: 180,
        endAngle: 0,
        min: 0,
        max: 100,
        splitNumber: 10,
        itemStyle: {
          color: function (params) {
            const colors = ['#F56C6C', '#E6A23C', '#909399', '#67C23A', '#409EFF']
            return colors[Math.floor(params.value / 20)] || colors[4]
          }
        },
        width: 30,
        pointer: {
          length: '50%',
          width: 8,
          itemStyle: {
            color: 'auto'
          }
        },
        axisLine: {
          lineStyle: {
            width: 30,
            color: themeStore.isDark ? 'rgba(255,255,255,0.2)' : 'rgba(0,0,0,0.1)'
          }
        },
        axisLabel: {
          color: themeStore.isDark ? '#909399' : '#606266',
          fontSize: 12,
          distance: -60,
          formatter: function (value) {
            return value + '%'
          }
        },
        detail: {
          valueAnimation: true,
          formatter: function (value) {
            return '{value}%'
          },
          fontSize: 36,
          offsetCenter: [0, '0%'],
          valueStyle: {
            color: themeStore.isDark ? '#fff' : '#333',
            fontSize: 42
          }
        },
        data: [{
          value: progress,
          name: '总体进度'
        }]
      }]
    }

    progressGaugeChartInstance.setOption(option)
  }

  // 清理图表
  function disposeCharts() {
    scoreTrendChartInstance?.dispose()
    masteryRadarChartInstance?.dispose()
    progressGaugeChartInstance?.dispose()
  }

  return {
    // State
    scoreTrendChartRef,
    masteryRadarChartRef,
    progressGaugeChartRef,
    loading,
    achievementsData,

    // Methods
    loadAchievementsData,
    initCharts,
    updateCharts,
    handleResize,
    disposeCharts
  }
}
