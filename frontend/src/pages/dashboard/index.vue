<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import CustomScroll from '../../components/CustomScroll.vue'
import { api } from '../../api/http'

const router = useRouter()

// 统计数据
const stats = ref({
  mastery: 0,
  points: 0,
  hours: 0
})

// 加载统计数据
async function loadStats() {
  try {
    // 获取测验统计
    const quizStats = await api.getQuizStats()
    console.log('Quiz stats:', quizStats)

    // 获取仪表盘数据（包含学习时长）
    const dashboardData = await api.getDashboard()
    console.log('Dashboard data:', dashboardData)

    // 计算平均掌握度
    let avgMastery = 0
    const knowledgeList = quizStats?.knowledgeMastery || quizStats?.knowledgePoints || []
    if (knowledgeList.length > 0) {
      const total = knowledgeList.reduce((sum, item) => sum + (item.masteryLevel || item.mastery || 0), 0)
      avgMastery = Math.round(total / knowledgeList.length)
    }

    // 知识点数量
    const knowledgePoints = knowledgeList.length || quizStats?.totalKnowledgePoints || 0

    // 学习时长（分钟转为小时）
    const totalMinutes = dashboardData?.durationDistribution?.totalMinutes
      || dashboardData?.totalMinutes
      || dashboardData?.totalLearningMinutes
      || 0
    const totalHours = Math.round(totalMinutes / 60)

    stats.value = {
      mastery: avgMastery,
      points: knowledgePoints,
      hours: totalHours
    }
  } catch (error) {
    console.error('Failed to load stats:', error)
    // 保持默认值 0
  }
}

// 最近文件
const recentFiles = ref([])

// 加载最近数据文件
async function loadRecentFiles() {
  try {
    const result = await api.dataPage({ page: 0, size: 5 })
    console.log('Data page result:', result)

    // 处理不同的返回结构 (Spring Page 使用 content 字段)
    const list = result?.content || result?.list || []
    console.log('Extracted list:', list)

    if (list && list.length > 0) {
      recentFiles.value = list.map(item => {
        console.log('Processing item:', item)
        return {
          name: item.fileName || item.fileOriginalName || item.originalName || item.name || '未知文件',
          date: formatDate(item.createdTime || item.createTime || item.uploadTime),
          status: item.processedStatus === 2 ? 'done' : 'processing'
        }
      })
    } else {
      // 如果没有数据，显示空状态提示
      recentFiles.value = []
    }
  } catch (error) {
    console.error('Failed to load recent files:', error)
    recentFiles.value = []
  }
}

// 格式化日期
function formatDate(dateStr) {
  if (!dateStr) return '未知时间'
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now - date
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)

  if (diffMins < 1) return '刚刚'
  if (diffMins < 60) return `${diffMins}分钟前`
  if (diffHours < 24) return `${diffHours}小时前`
  if (diffDays < 7) return `${diffDays}天前`
  return date.toLocaleDateString()
}

// 每日测验
const quizQuestion = ref('')
const quizOptions = ref([])
const hasQuiz = ref(false)

// 加载每日测验
async function loadDailyQuiz() {
  try {
    const recommendations = await api.getReviewRecommendation()
    if (recommendations && recommendations.length > 0) {
      const quiz = recommendations[0]
      quizQuestion.value = quiz.question || quiz.content || ''
      quizOptions.value = (quiz.options || []).map((opt, idx) => ({
        text: opt,
        selected: false,
        correct: idx === quiz.correctIndex
      }))
      hasQuiz.value = true
    } else {
      hasQuiz.value = false
    }
  } catch (error) {
    console.error('Failed to load daily quiz:', error)
    hasQuiz.value = false
  }
}

// 热门概念
const topConcepts = ref([])

// 加载热门概念
async function loadTopConcepts() {
  try {
    const wordCloud = await api.getWordReport()
    if (wordCloud && typeof wordCloud === 'object') {
      // 取词云频率最高的前9个
      topConcepts.value = Object.entries(wordCloud)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 9)
        .map(([word]) => word)
    } else {
      topConcepts.value = []
    }
  } catch (error) {
    console.error('Failed to load top concepts:', error)
    topConcepts.value = []
  }
}

// 用户数据
const user = ref({
  name: '',
  avatar: ''
})

// 加载用户信息
async function loadUserInfo() {
  try {
    const userInfo = await api.getUserInfo()
    if (userInfo) {
      user.value = {
        name: userInfo.nickname || userInfo.username || '用户',
        avatar: userInfo.avatar || ''
      }
    }
  } catch (error) {
    console.error('Failed to load user info:', error)
    user.value.name = '用户'
  }
}

// 技能雷达数据
const skills = ref([])

// 活动数据
const activityData = ref([])
const hasActivity = ref(false)

// 加载技能数据
async function loadSkills() {
  try {
    const knowledgeMastery = await api.getKnowledgeMastery(5)
    if (knowledgeMastery && knowledgeMastery.length > 0) {
      skills.value = knowledgeMastery.map(item => ({
        name: item.tagName || item.knowledgePoint || item.name || '未知技能',
        value: item.masteryLevel || item.mastery || 0
      }))
    } else {
      skills.value = []
    }
  } catch (error) {
    console.error('Failed to load skills:', error)
    skills.value = []
  }
}

// 加载活动数据
async function loadActivity() {
  try {
    const dashboardData = await api.getDashboard()
    const heatmap = dashboardData?.heatmap || dashboardData?.activityHeatmap || []

    if (heatmap && heatmap.length > 0) {
      // 取最近7天的数据
      activityData.value = heatmap.slice(-7).map(day => ({
        day: day.day || day.date,
        value: day.value || day.count || day.intensity || 0
      }))
      hasActivity.value = activityData.value.some(d => d.value > 0)
    } else {
      activityData.value = []
      hasActivity.value = false
    }
  } catch (error) {
    console.error('Failed to load activity:', error)
    activityData.value = []
    hasActivity.value = false
  }
}

function handleImportLog() {
  router.push('/data')
}

function handleStartQuiz() {
  router.push('/quiz-history')
}

function handleSkipQuiz() {
  // 跳过当前题目
}

function handleProfileClick() {
  router.push('/profile')
}

function handleQuizOption(option, index) {
  quizOptions.value.forEach((opt, i) => {
    opt.selected = i === index
  })
}

function getStatusColor(status) {
  return status === 'processing' ? '#f59e0b' : '#CC6633'
}

onMounted(async () => {
  // 加载统计数据
  loadStats()

  // 加载最近文件
  loadRecentFiles()

  // 加载用户信息
  loadUserInfo()

  // 加载技能数据
  loadSkills()

  // 加载热门概念
  loadTopConcepts()

  // 加载每日测验
  loadDailyQuiz()

  // 加载活动数据
  loadActivity()
})
</script>

<template>
  <div class="dashboard-page">
    <CustomScroll class="dashboard-scroll">
      <!-- Three Column Layout -->
      <div class="dashboard-grid">
        <!-- Left Column -->
        <div class="col-left">
          <!-- Profile Card -->
          <section class="card glass profile-card" @click="handleProfileClick">
            <div class="profile-summary">
              <div class="avatar"></div>
              <div class="profile-info">
                <span class="profile-name">{{ user.name }}</span>
              </div>
            </div>
            <div class="badges-row">
              <div class="badge" title="Fast Learner">⚡</div>
              <div class="badge" title="Deep Diver">🌊</div>
              <div class="badge" title="Consistent">🔥</div>
              <div class="badge" title="Master">🏆</div>
            </div>
          </section>

          <!-- Data Source -->
          <section class="card glass data-source-card">
            <div class="card-header">
              <span class="card-title">数据来源</span>
              <span class="card-action" @click="handleImportLog">+</span>
            </div>
            <div class="file-list">
              <div v-if="recentFiles.length === 0" class="empty-files">
                <span class="empty-text">暂无数据文件</span>
                <span class="empty-hint">点击 + 导入</span>
              </div>
              <div v-for="file in recentFiles" :key="file.name" class="file-item">
                <div class="file-info">
                  <span class="file-name">{{ file.name }}</span>
                  <span class="file-date">{{ file.date }}</span>
                </div>
                <div class="status-dot" :style="{ backgroundColor: getStatusColor(file.status) }"></div>
              </div>
            </div>
          </section>
        </div>

        <!-- Center Column -->
        <div class="col-center">
          <!-- Stats Grid -->
          <div class="stats-grid">
            <section class="card glass stat-card">
              <div class="stat-value">{{ stats.mastery }}%</div>
              <div class="stat-label">掌握度</div>
            </section>
            <section class="card glass stat-card">
              <div class="stat-value">{{ stats.points }}</div>
              <div class="stat-label">知识点</div>
            </section>
            <section class="card glass stat-card">
              <div class="stat-value">{{ stats.hours }}h</div>
              <div class="stat-label">学习时长</div>
            </section>
          </div>

          <!-- Skills Section -->
          <section class="card glass skills-card">
            <div class="card-header">
              <span class="card-title">技能掌握</span>
            </div>
            <div class="skills-list">
              <div v-if="skills.length === 0" class="empty-skills">
                <span class="empty-text">暂无技能数据</span>
                <span class="empty-hint">完成测验以构建你的技能档案</span>
              </div>
              <div v-for="skill in skills" :key="skill.name" class="skill-item">
                <div class="skill-header">
                  <span class="skill-name">{{ skill.name }}</span>
                  <span class="skill-value">{{ skill.value }}%</span>
                </div>
                <div class="progress-bar-bg">
                  <div class="progress-bar-fill" :style="{ width: skill.value + '%' }"></div>
                </div>
              </div>
            </div>
          </section>

          <!-- Top Concepts -->
          <section class="card glass concepts-card">
            <div class="card-header">
              <span class="card-title">热门概念</span>
            </div>
            <div class="concepts-grid">
              <div v-if="topConcepts.length === 0" class="empty-concepts">
                <span class="empty-text">暂无概念</span>
                <span class="empty-hint">导入数据以发现你的概念</span>
              </div>
              <span v-for="concept in topConcepts" :key="concept" class="concept-tag">
                {{ concept }}
              </span>
            </div>
          </section>
        </div>

        <!-- Right Column -->
        <div class="col-right">
          <!-- Daily Quiz -->
          <section class="card glass quiz-card">
            <div class="card-header">
              <span class="card-title">每日测验</span>
              <span v-if="hasQuiz" class="card-action" @click="handleSkipQuiz">跳过</span>
            </div>
            <div class="quiz-content">
              <div v-if="!hasQuiz" class="empty-quiz">
                <span class="empty-text">暂无测验</span>
                <span class="empty-hint">添加错题以生成每日测验</span>
              </div>
              <template v-else>
                <div class="quiz-question">{{ quizQuestion }}</div>
                <div class="quiz-options">
                  <div
                    v-for="(option, index) in quizOptions"
                    :key="index"
                    class="quiz-option"
                    :class="{ selected: option.selected }"
                    @click="handleQuizOption(option, index)"
                  >
                    <span>{{ option.text }}</span>
                    <span v-if="option.correct && option.selected" class="check-mark">✓</span>
                  </div>
                </div>
                <div class="quiz-progress">
                  <div class="quiz-bar" style="width: 65%;"></div>
                </div>
                <div class="quiz-status">第 3/5 题</div>
              </template>
            </div>
          </section>

          <!-- Learning Activity -->
          <section class="card glass activity-card">
            <div class="card-header">
              <span class="card-title">学习活动</span>
            </div>
            <div v-if="!hasActivity" class="empty-activity">
              <span class="empty-text">暂无活动</span>
              <span class="empty-hint">开始学习以追踪进度</span>
            </div>
            <template v-else>
              <div class="activity-chart">
                <div
                  v-for="(day, index) in activityData"
                  :key="index"
                  class="activity-bar"
                  :class="{ active: day.value > 50 }"
                  :style="{ height: Math.min(day.value, 100) + '%' }"
                ></div>
              </div>
              <div class="activity-labels">
                <span v-for="(day, index) in activityData" :key="index">
                  {{ day.day?.slice(0, 3) || ['周一', '周二', '周三', '周四', '周五', '周六', '周日'][index] }}
                </span>
              </div>
            </template>
          </section>
        </div>
      </div>
    </CustomScroll>
  </div>
</template>

<style scoped lang="scss">
// Glass Effect
.glass {
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
}

// Container
.dashboard-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

// Scroll Area
.dashboard-scroll {
  flex: 1;
  padding: 24px 32px;
}

// Grid Layout
.dashboard-grid {
  display: grid;
  grid-template-columns: 320px 1fr 380px;
  gap: 24px;
  height: 100%;
  max-width: 1600px;
  margin: 0 auto;
}

// Columns
.col-left {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.col-center {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.col-right {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

// Card Styles
.card {
  border-radius: 24px;
  padding: 24px;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.card-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 4px;
}

.card-action {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s;
  font-weight: 500;
}

.card-action:hover {
  color: var(--text-primary);
}

// Profile Section
.profile-card {
  min-height: 160px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.profile-card:hover {
  background: rgba(255, 248, 245, 0.08);
  border-color: rgba(255, 248, 245, 0.15);
  transform: translateY(-2px);
}

.profile-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--gradient-warm);
  border: 2px solid rgba(255, 255, 255, 0.2);
  flex-shrink: 0;
}

.profile-name {
  font-size: 18px;
  font-weight: 600;
  display: block;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.profile-level {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.badges-row {
  display: flex;
  gap: 10px;
}

.badge {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 248, 245, 0.05);
  border: 1px solid rgba(255, 248, 245, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.badge:hover {
  transform: scale(1.1);
  background: rgba(255, 248, 245, 0.1);
}

// Data Source
.data-source-card {
  flex: 1;
  min-height: 300px;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-files {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 8px;
  color: rgba(255, 255, 255, 0.4);
}

.empty-text {
  font-size: 14px;
  font-weight: 500;
}

.empty-hint {
  font-size: 12px;
  opacity: 0.7;
}

// Skills empty state
.empty-skills {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 8px;
  color: rgba(255, 255, 255, 0.4);
}

// Concepts empty state
.empty-concepts {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px 20px;
  gap: 8px;
  color: rgba(255, 255, 255, 0.4);
}

// Quiz empty state
.empty-quiz {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  gap: 8px;
  color: rgba(255, 255, 255, 0.4);
}

// Activity empty state
.empty-activity {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 50px 20px;
  gap: 8px;
  color: rgba(255, 255, 255, 0.4);
}

.file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-radius: 16px;
  background: rgba(255, 248, 245, 0.03);
  border: 1px solid rgba(255, 248, 245, 0.05);
  transition: all 0.2s;
}

.file-item:hover {
  background: rgba(255, 248, 245, 0.06);
  border-color: rgba(255, 248, 245, 0.1);
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.file-date {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

// Stats Grid
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat-card {
  text-align: center;
  padding: 24px 16px;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.stat-value {
  font-size: 36px;
  font-weight: 600;
  margin-bottom: 8px;
  background: var(--gradient-warm);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-label {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  font-weight: 500;
}

// Skills Section
.skills-card {
  flex: 1;
  min-height: 280px;
}

.skills-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.skill-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.skill-header {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
}

.skill-name {
  color: var(--text-primary);
}

.skill-value {
  color: var(--accent-tertiary);
  font-weight: 600;
}

.progress-bar-bg {
  height: 6px;
  background: rgba(255, 248, 245, 0.05);
  border-radius: 3px;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: var(--gradient-warm);
  border-radius: 3px;
  box-shadow: 0 0 10px var(--accent-glow-soft);
  transition: width 0.5s ease;
}

// Concepts
.concepts-card {
  min-height: 140px;
}

.concepts-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.concept-tag {
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 12px;
  background: rgba(255, 248, 245, 0.05);
  border: 1px solid rgba(255, 248, 245, 0.08);
  color: var(--text-secondary);
  transition: all 0.2s;
}

.concept-tag:hover {
  background: rgba(204, 102, 51, 0.1);
  border-color: rgba(204, 102, 51, 0.3);
  color: var(--accent-tertiary);
}

// Quiz Section
.quiz-card {
  min-height: 240px;
}

.quiz-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quiz-question {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.quiz-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quiz-option {
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid var(--glass-border);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 248, 245, 0.02);
}

.quiz-option:hover {
  background: rgba(255, 248, 245, 0.05);
  border-color: rgba(255, 248, 245, 0.2);
}

.quiz-option.selected {
  background: rgba(204, 102, 51, 0.15);
  border-color: rgba(204, 102, 51, 0.5);
  color: white;
}

.check-mark {
  color: #22c55e;
  font-weight: bold;
  font-size: 14px;
}

.quiz-progress {
  height: 3px;
  background: rgba(255, 248, 245, 0.1);
  border-radius: 2px;
  margin-top: 8px;
  overflow: hidden;
}

.quiz-bar {
  width: 65%;
  height: 100%;
  background: var(--gradient-warm);
  border-radius: 2px;
  transition: width 0.3s ease;
}

.quiz-status {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  text-align: right;
}

// Activity Chart
.activity-card {
  flex: 1;
  min-height: 200px;
}

.activity-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 120px;
  padding: 16px 0;
  gap: 8px;
}

.activity-bar {
  flex: 1;
  background: rgba(255, 248, 245, 0.1);
  border-radius: 4px 4px 0 0;
  min-width: 20px;
  transition: all 0.2s;
}

.activity-bar:hover,
.activity-bar.active {
  background: var(--gradient-warm);
  box-shadow: 0 0 12px var(--accent-glow-soft);
}

.activity-labels {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

// Responsive
@media (max-width: 1400px) {
  .dashboard-grid {
    grid-template-columns: 280px 1fr 340px;
    gap: 20px;
  }

  .dashboard-scroll {
    padding: 20px 24px;
  }
}

@media (max-width: 1200px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .col-left, .col-right {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
  }

  .col-center {
    order: -1;
  }
}

@media (max-width: 768px) {
  .dashboard-scroll {
    padding: 16px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
