import { ref } from 'vue'
import { api } from '../../../api/http'

// 学习成就相关状态（简化版 - 图表组件已自管理）
export function useAchievements() {
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
    loading.value.trends = true
    loading.value.progress = true
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
    } catch (e) {
      console.error('加载学习成就数据失败:', e)
    } finally {
      loading.value.achievements = false
      loading.value.trends = false
      loading.value.progress = false
    }
  }

  return {
    // State
    loading,
    achievementsData,

    // Methods
    loadAchievementsData
  }
}
