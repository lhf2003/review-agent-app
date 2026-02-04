<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { TrendCharts, Document, WarningFilled } from '@element-plus/icons-vue'
import QuizHistoryPage from './QuizHistoryPage.vue'
import QuizDetailPage from './QuizDetailPage.vue'
import MistakeListPane from '../../components/quiz/MistakeListPane.vue'
import MistakeDetailPane from '../../components/quiz/MistakeDetailPane.vue'
import TrendsSection from '../../components/quiz/TrendsSection.vue'
import { useAchievements } from '../profile/composables/useAchievements'
import { api } from '../../api/http'

const router = useRouter()
const activeView = ref('history') // history, trends, mistake
const currentQuizId = ref('')
const mistakeCount = ref(0)

// 错题本 split-layout 状态
const currentMistakeId = ref(null)
const currentQuestionId = ref(null)
const mistakeListPaneRef = ref(null)

// 复用成就数据逻辑
const { achievementsData, loading, loadAchievementsData } = useAchievements()

function handleSelectQuiz(quizId) {
  currentQuizId.value = quizId
  // In split view, we stay in history view, just update the selected ID
}

function handleGoBack() {
  currentQuizId.value = ''
}

// 错题本事件处理
function handleSelectMistake(mistake) {
  currentMistakeId.value = mistake.id
  currentQuestionId.value = mistake.questionId
}

function handleMarkedMastered(questionId) {
  mistakeListPaneRef.value?.refreshMistakes()
  currentMistakeId.value = null
  currentQuestionId.value = null
}

function handleDeleted(questionId) {
  mistakeListPaneRef.value?.refreshMistakes()
  currentMistakeId.value = null
  currentQuestionId.value = null
}

async function loadMistakeCount() {
  try {
    const stats = await api.getMistakeStats()
    mistakeCount.value = stats.totalCount || 0
  } catch (error) {
    console.error('加载错题数量失败:', error)
  }
}

onMounted(() => {
  loadAchievementsData()
  loadMistakeCount()
})
</script>

<template>
  <div class="app-root">
    <!-- 顶部导航 -->
    <div class="top-nav-container">
       <div class="nav-left">
         <el-radio-group v-model="activeView" class="nav-radio-group">
           <el-radio-button value="history">习题历史</el-radio-button>
           <el-radio-button value="mistake">
             错题本
             <span v-if="mistakeCount > 0" class="mistake-badge-text">({{ mistakeCount }})</span>
           </el-radio-button>
          <el-radio-button value="trends">趋势分析</el-radio-button>
         </el-radio-group>
       </div>
    </div>

    <!-- 主内容区 -->
    <main class="content-area">
      <Transition name="fade" mode="out-in">
        <!-- 习题历史 (Split View) -->
        <div v-if="activeView === 'history'" key="history" class="view-container split-layout">
           <!-- 左侧列表 -->
           <div class="list-pane glass-panel">
             <QuizHistoryPage :embedded="true" @select-quiz="handleSelectQuiz" />
           </div>
           
           <!-- 右侧详情 -->
           <div class="detail-pane glass-panel">
             <Transition name="fade" mode="out-in">
               <QuizDetailPage 
                 v-if="currentQuizId" 
                 :key="currentQuizId" 
                 :quizId="currentQuizId" 
                 :embedded="true"
                 @go-back="handleGoBack" 
               />
               <div v-else class="empty-detail-state">
                 <div class="empty-icon-wrapper">
                   <el-icon :size="48"><Document /></el-icon>
                 </div>
                 <h3>选择习题查看详情</h3>
                 <p>点击左侧列表中的习题记录，在此处查看详细解析</p>
               </div>
             </Transition>
           </div>
        </div>
        
        <!-- 趋势分析 -->
        <div v-else-if="activeView === 'trends'" key="trends" class="view-container">
           <div class="trends-wrapper">
             <div class="page-header">
               <h1 class="page-title">学习趋势分析</h1>
               <p class="page-subtitle">追踪您的学习进度与知识掌握情况</p>
             </div>
             <TrendsSection :achievements-data="achievementsData" :loading="loading" />
           </div>
        </div>

        <!-- 错题本 -->
        <div v-else-if="activeView === 'mistake'" key="mistake" class="view-container split-layout">
           <!-- 左侧列表 -->
           <div class="list-pane glass-panel">
             <MistakeListPane
               ref="mistakeListPaneRef"
               :embedded="true"
               @select-mistake="handleSelectMistake"
             />
           </div>

           <!-- 右侧详情 -->
           <div class="detail-pane glass-panel">
             <Transition name="fade" mode="out-in">
               <MistakeDetailPane
                 v-if="currentMistakeId"
                 :key="currentQuestionId"
                 :mistake-id="currentMistakeId"
                 :question-id="currentQuestionId"
                 @marked-mastered="handleMarkedMastered"
                 @deleted="handleDeleted"
               />
               <div v-else class="empty-detail-state">
                 <div class="empty-icon-wrapper">
                   <el-icon :size="48"><Document /></el-icon>
                 </div>
                 <h3>选择错题查看详情</h3>
                 <p>点击左侧列表中的错题，在此处查看完整解析和掌握情况</p>
               </div>
             </Transition>
           </div>
        </div>
      </Transition>
    </main>
  </div>
</template>

<style scoped>
.app-root {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden;
  gap: 16px;
  padding: 0 4px;
  min-height: 0;
}

/* ============ Top Nav ============ */
.top-nav-container {
  padding: 4px 0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 10;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.nav-radio-group {
  --el-fill-color-light: rgba(255, 255, 255, 0.5);
  --el-border-radius-base: 8px;
}

.nav-radio-group :deep(.el-radio-button__inner) {
  border: none;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  border-radius: 8px;
  padding: 6px 12px;
  min-width: 90px;
  font-size: 14px;
}

.nav-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: var(--el-color-primary);
  color: white;
  box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.3);
}

/* ============ Mistake Badge ============ */
.mistake-badge-text {
  margin-left: 4px;
  color: #ff3b30;
  font-weight: 600;
  font-size: 12px;
}

.nav-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) .mistake-badge-text {
  color: rgba(255, 255, 255, 0.9);
}

/* ============ Content Area ============ */
.content-area {
  flex: 1;
  padding: 0;
  overflow: hidden;
  position: relative;
  min-height: 0;
}

.view-container {
  height: 100%;
  width: 100%;
}

/* ============ Split Layout ============ */
.split-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 16px;
  height: 100%;
  overflow: visible; /* Allow shadow to be visible */
  padding: 4px;
}

.list-pane {
  height: 100%;
  overflow: hidden;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
}

.detail-pane {
  height: 100%;
  overflow: hidden; /* Detail page has its own scroll */
  border-radius: 16px;
  position: relative;
  display: flex;
  flex-direction: column;
}

/* Adjust inner components to fit pane */
.detail-pane :deep(.quiz-detail-page) {
  /* 移除所有覆盖性设置，让组件保持自己的样式 */
  flex: 1;
  min-height: 0;
}

/* MistakeBookPage 已有自己的布局，不需要额外宽度设置 */

/* Scrollbar for detail pane - Not needed as inner list scrolls */
/* Empty State */
.empty-detail-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  text-align: center;
  padding: 40px;
}

.empty-icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.03);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  color: var(--el-text-color-placeholder);
}

.empty-detail-state h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--el-text-color-primary);
}

.empty-detail-state p {
  font-size: 14px;
  margin: 0;
  max-width: 300px;
}

/* ============ Trends Wrapper ============ */
.trends-wrapper {
  height: 100%;
  overflow-y: auto;
  border-radius: 16px;
  max-width: 1200px;
  margin: 0 auto;
  box-sizing: border-box;
  padding: 16px;
}

.embedded-page-wrapper {
  height: 100%;
  width: 100%;
  overflow: hidden;
  border-radius: 16px;
  box-sizing: border-box;
  padding: 16px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px 0;
  color: var(--el-text-color-primary);
}

.page-subtitle {
  font-size: 16px;
  color: var(--el-text-color-secondary);
  margin: 0;
}

/* ============ Glassmorphism ============ */
.glass-panel {
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow:
    0 4px 24px -1px rgba(0, 0, 0, 0.06),
    0 0 0 1px rgba(255, 255, 255, 0.4) inset;
}

/* ============ Transitions ============ */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* ============ Responsive ============ */
@media (max-width: 1024px) {
  .split-layout {
    grid-template-columns: 280px 1fr;
  }
}

@media (max-width: 768px) {
  .split-layout {
    grid-template-columns: 1fr; /* Stack on mobile, or hide list when detail is open */
  }
  
  .detail-pane {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 100;
    transform: translateX(100%);
    transition: transform 0.3s ease;
  }
  
  /* Need a way to show detail pane on mobile, but for now assuming desktop focus */
}

/* ============ html.dark 深色模式兼容 ============ */
html.dark .app-root {
  background-color: #000000;
}

html.dark .glass-panel {
  background: rgba(28, 28, 30, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.5),
    0 0 0 1px rgba(255, 255, 255, 0.08) inset;
}

html.dark .detail-pane :deep(.quiz-detail-page)::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, 0.15);
}

html.dark .nav-radio-group :deep(.el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.1);
  color: var(--el-text-color-regular);
}

html.dark .nav-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: var(--el-color-primary);
  color: white;
}

html.dark .empty-icon-wrapper {
  background: rgba(255, 255, 255, 0.05);
}
</style>