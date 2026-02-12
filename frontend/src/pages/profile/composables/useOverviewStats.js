import { ref } from 'vue'
import { api } from '../../../api/http'

/**
 * 学习数据概览统计组合式函数
 * 提供个人中心数据概览所需的数据获取逻辑
 */
export function useOverviewStats() {
  const stats = ref({
    totalDays: 0,
    totalQuizzes: 0,
    avgScore: 0,
    totalMistakes: 0
  })
  const loading = ref(false)
  const error = ref(null)

  /**
   * 计算总学习天数
   * 基于仪表盘数据和用户注册时间
   */
  function calculateTotalDays(dashboardData) {
    // TODO: 从后端获取准确的用户注册时间
    // 临时方案：使用最近学习记录的时间跨度
    if (!dashboardData || !dashboardData.heatmap) return 0

    const dates = Object.keys(dashboardData.heatmap)
    if (dates.length === 0) return 0

    const firstDate = new Date(Math.min(...dates.map(d => new Date(d).getTime())))
    const lastDate = new Date(Math.max(...dates.map(d => new Date(d).getTime())))

    const diffTime = lastDate - firstDate
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

    return diffDays
  }

  /**
   * 加载概览统计数据
   * 聚合多个 API 数据源
   */
  async function loadOverviewStats() {
    loading.value = true
    error.value = null

    try {
      // 并行请求多个数据源
      const [mistakeStats, quizStats, dashboardData] = await Promise.all([
        api.getMistakeStats().catch(err => {
          console.warn('获取错题统计失败，使用默认值:', err)
          return { totalCount: 0, unmastered: 0 }
        }),
        api.getQuizStats().catch(err => {
          console.warn('获取测验统计失败，使用默认值:', err)
          return { totalCount: 0, avgScore: 0 }
        }),
        api.getDashboard().catch(err => {
          console.warn('获取仪表盘数据失败，使用默认值:', err)
          return null
        })
      ])

      // 计算总学习天数
      const totalDays = dashboardData ? calculateTotalDays(dashboardData) : 0

      // 更新统计数据
      stats.value = {
        totalDays,
        totalQuizzes: quizStats?.totalCount || 0,
        avgScore: quizStats?.avgScore || 0,
        totalMistakes: mistakeStats?.unmastered || 0
      }

      console.log('[OverviewStats] 数据加载成功:', stats.value)
    } catch (err) {
      console.error('加载概览统计失败:', err)
      error.value = err.message || '加载数据失败'
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * 刷新统计数据
   */
  async function refreshStats() {
    await loadOverviewStats()
  }

  return {
    stats,
    loading,
    error,
    loadOverviewStats,
    refreshStats
  }
}
