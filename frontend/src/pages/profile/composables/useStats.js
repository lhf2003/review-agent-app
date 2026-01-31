import { ref } from 'vue'
import { api } from '../../../api/http'
import { ElMessage } from 'element-plus'

export function useStats() {
  const stats = ref({
    syncFileCount: 0,
    analyzedCount: 0,
    collectionCount: 0,
    tagCount: 0,
    quizCompletedCount: 0,
    learningDays: 0,
    recentActivities: [],
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

  const animatedStats = ref({
    syncFileCount: 0,
    analyzedCount: 0,
    collectionCount: 0,
    tagCount: 0,
    quizCompletedCount: 0,
    learningDays: 0
  })

  async function loadStats() {
    try {
      const resp = await api.getUserStats()
      stats.value = resp?.data || resp || {
        syncFileCount: 0,
        analyzedCount: 0,
        collectionCount: 0,
        tagCount: 0,
        quizCompletedCount: 0,
        learningDays: 0,
        recentActivities: []
      }
      animateNumbers()
    } catch (e) {
      ElMessage.error('加载统计数据失败: ' + e.message)
      stats.value = {
        syncFileCount: 0,
        analyzedCount: 0,
        collectionCount: 0,
        tagCount: 0,
        quizCompletedCount: 0,
        learningDays: 0,
        recentActivities: []
      }
    }
  }

  function animateNumbers() {
    const duration = 1500
    const keys = ['syncFileCount', 'analyzedCount', 'collectionCount', 'tagCount', 'quizCompletedCount', 'learningDays']

    keys.forEach(key => {
      const target = stats.value[key]
      const start = 0
      const startTime = performance.now()

      function update(currentTime) {
        const elapsed = currentTime - startTime
        const progress = Math.min(elapsed / duration, 1)
        const easeProgress = 1 - Math.pow(1 - progress, 4)
        animatedStats.value[key] = Math.floor(start + (target - start) * easeProgress)

        if (progress < 1) {
          requestAnimationFrame(update)
        } else {
          animatedStats.value[key] = target
        }
      }

      requestAnimationFrame(update)
    })
  }

  function getActivityIcon(type) {
    const icons = {
      sync: 'Document',
      collection: 'FolderOpened',
      quiz: 'Edit',
      report: 'Notebook'
    }
    return icons[type] || 'Document'
  }

  const statCards = [
    { key: 'syncFileCount', label: '已同步文件', icon: 'Document', color: '#409EFF', trend: null },
    { key: 'analyzedCount', label: '已分析结果', icon: 'DataLine', color: '#67C23A', trend: null },
    { key: 'collectionCount', label: '合集数量', icon: 'Collection', color: '#E6A23C', trend: null },
    { key: 'tagCount', label: '标签数量', icon: 'CollectionTag', color: '#F56C6C', trend: null },
    { key: 'quizCompletedCount', label: '测验完成', icon: 'CircleCheck', color: '#909399', trend: null },
    { key: 'learningDays', label: '学习天数', icon: 'Calendar', color: '#409EFF', trend: null }
  ]

  const quickActions = [
    { label: '我的合集', icon: 'FolderOpened', color: '#409EFF', path: '/collections' },
    { label: '待分析', icon: 'Document', color: '#67C23A', path: '/data' },
    { label: '最近报告', icon: 'Notebook', color: '#E6A23C', path: '/report' },
    { label: '学习测验', icon: 'Edit', color: '#F56C6C', path: '/collections' },
    { label: '标签管理', icon: 'PriceTag', color: '#909399', path: '/tags' },
    { label: '数据统计', icon: 'TrendCharts', color: '#409EFF', path: '/word-cloud' },
    { label: '同步历史', icon: 'Clock', color: '#67C23A', path: '/sync' },
    { label: '系统配置', icon: 'Setting', color: '#E6A23C', path: '/config' }
  ]

  return {
    stats,
    animatedStats,
    loadStats,
    getActivityIcon,
    statCards,
    quickActions
  }
}
