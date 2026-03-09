<template>
  <div class="learning-dashboard">
    <CustomScroll class="dashboard-scroll">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2>学习数据</h2>
        <p class="subtitle">追踪你的学习进度，发现知识薄弱点</p>
      </div>

      <!-- 周统计卡片 -->
      <div class="weekly-stats-section">
        <WeeklyStatsCard :stats="dashboardData.weeklyStats" :loading="loading" />
      </div>

      <!-- 图表网格区域 -->
      <div class="charts-grid">
        <!-- 测验分数趋势图 -->
        <div class="chart-card">
          <div class="chart-header">
            <h3>测验分数趋势</h3>
            <el-radio-group v-model="trendRange" size="small" @change="loadTrendData">
              <el-radio-button label="7">7天</el-radio-button>
              <el-radio-button label="30">30天</el-radio-button>
              <el-radio-button label="90">90天</el-radio-button>
              <el-radio-button label="all">全部</el-radio-button>
            </el-radio-group>
          </div>
          <ScoreTrendChart
            :data="dashboardData.scoreTrend"
            :loading="loading"
            height="280px"
          />
        </div>

        <!-- 知识点掌握度雷达图 -->
        <div class="chart-card">
          <div class="chart-header">
            <h3>知识点掌握度</h3>
          </div>
          <KnowledgeRadarChart
            :data="knowledgeRadarData"
            :loading="loading"
            height="280px"
          />
        </div>

        <!-- 学习热力图 -->
        <div class="chart-card full-width">
          <div class="chart-header">
            <h3>学习热力图</h3>
            <span class="chart-tip">最近12个月的学习记录</span>
          </div>
          <LearningHeatmapChart
            :data="dashboardData.heatmap"
            :loading="loading"
            height="200px"
            @date-click="handleDateClick"
          />
        </div>

        <!-- 学习时长分布图 -->
        <div class="chart-card">
          <div class="chart-header">
            <h3>学习时段分布</h3>
          </div>
          <TimeDistributionChart
            :data="dashboardData.timeDistribution"
            :loading="loading"
            height="280px"
          />
        </div>
      </div>
    </CustomScroll>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CustomScroll from '../../../components/CustomScroll.vue'
import ScoreTrendChart from '../../../components/charts/ScoreTrendChart.vue'
import KnowledgeRadarChart from '../../../components/charts/KnowledgeRadarChart.vue'
import LearningHeatmapChart from '../../../components/charts/LearningHeatmapChart.vue'
import TimeDistributionChart from '../../../components/charts/TimeDistributionChart.vue'
import WeeklyStatsCard from '../../../components/charts/WeeklyStatsCard.vue'
import { quizApi } from '../../../api'

const router = useRouter()

// 状态
const loading = ref(false)
const trendRange = ref('30')

// 仪表盘数据
const dashboardData = ref({
  scoreTrend: [],
  knowledgeRadar: [],
  heatmap: {},
  timeDistribution: {},
  weeklyStats: {
    thisWeekQuizCount: 0,
    lastWeekQuizCount: 0,
    thisWeekAccuracy: 0,
    lastWeekAccuracy: 0
  }
})

// ========== 临时测试数据 ==========
// 用于验证图表组件是否正常工作
// 如果需要启用，取消下面 4 行的注释，刷新页面即可

const testDashboardData = {
  scoreTrend: [
    {date: '2026-02-01', score: 80, quizId: 1, collectionName: '测试合集1'},
    {date: '2026-02-02', score: 85, quizId: 2, collectionName: '测试合集2'}
  ],
  knowledgeRadar: [
    {name: '测试知识点1', value: 80, totalCount: 10},
    {name: '测试知识点2', value: 90, totalCount: 15}
  ]
}

// 测试模式：2 秒后使用测试数据
const TEST_MODE = false  // 改为 true 启用

if (TEST_MODE) {
  setTimeout(() => {
    
    dashboardData.value = testDashboardData
  }, 2000)
}
// ========== 临时测试数据结束 ==========

// 转换知识点数据格式以适配 KnowledgeRadarChart
const knowledgeRadarData = computed(() => {
  return (dashboardData.value.knowledgeRadar || []).map(item => ({
    tagName: item.name,
    accuracyRate: item.value
  }))
})

// 加载仪表盘数据
async function loadDashboardData() {
  loading.value = true
  try {
    const data = await quizApi.getDashboard()
    
    
    

    if (data) {
      dashboardData.value = {
        scoreTrend: data.scoreTrend || [],
        knowledgeRadar: data.knowledgeRadar || [],
        heatmap: data.heatmap || {},
        timeDistribution: data.timeDistribution || {},
        weeklyStats: data.weeklyStats || {
          thisWeekQuizCount: 0,
          lastWeekQuizCount: 0,
          thisWeekAccuracy: 0,
          lastWeekAccuracy: 0
        }
      }

      
    }
  } catch (e) {
    
    ElMessage.error('加载学习数据失败')
  } finally {
    loading.value = false
  }
}

// 加载趋势数据（按时间范围）
async function loadTrendData() {
  try {
    const data = await quizApi.getDashboardTrend(trendRange.value)
    if (data) {
      dashboardData.value.scoreTrend = data || []
    }
  } catch (e) {
    
  }
}

// 点击热力图日期
function handleDateClick(date) {
  // 跳转到该日期的测验历史
  router.push({
    path: '/quiz-history',
    query: { date }
  })
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped lang="scss">
.learning-dashboard {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.dashboard-scroll {
  flex: 1;
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

.page-header {
  margin-bottom: 24px;

  h2 {
    font-size: 24px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin: 0 0 8px 0;
  }

  .subtitle {
    font-size: 14px;
    color: var(--el-text-color-secondary);
    margin: 0;
  }
}

.weekly-stats-section {
  margin-bottom: 24px;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.chart-card {
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  padding: 20px;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
}

.chart-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.chart-card.full-width {
  grid-column: 1 / -1;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;

  h3 {
    font-size: 16px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    margin: 0;
  }

  .chart-tip {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}

/* 响应式布局 */
@media (max-width: 1024px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }

  .chart-card.full-width {
    grid-column: 1;
  }
}

@media (max-width: 768px) {
  .dashboard-scroll {
    padding: 16px;
  }

  .page-header h2 {
    font-size: 20px;
  }

  .chart-card {
    padding: 16px;
  }

  .chart-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}

/* 大屏幕 */
@media (min-width: 1600px) {
  .dashboard-scroll {
    max-width: 1600px;
  }

  .charts-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
