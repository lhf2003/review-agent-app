<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document } from '@element-plus/icons-vue'
import QuizHistoryPage from './QuizHistoryPage.vue'
import QuizDetailPage from './QuizDetailPage.vue'
import MistakeListPane from '../../components/quiz/MistakeListPane.vue'
import MistakeDetailPane from '../../components/quiz/MistakeDetailPane.vue'
import ReviewModePane from '../../components/quiz/ReviewModePane.vue'
import RecommendationsPage from './RecommendationsPage.vue'
import { api } from '../../api/http'

const router = useRouter()
const activeView = ref('recommendations') // recommendations, history, mistake, mistake-review - 智能推荐为默认视图
const currentQuizId = ref('')
const mistakeCount = ref(0)
const recommendationCount = ref(0)

// 错题本 split-layout 状态
const currentMistakeId = ref(null)
const currentQuestionId = ref(null)
const mistakeListPaneRef = ref(null)

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
    
  }
}

// 加载推荐数量
async function loadRecommendationCount() {
  try {
    const stats = await api.getMistakeStats()
    // 使用未掌握的错题数量作为推荐数量
    recommendationCount.value = stats.unmastered || 0
  } catch (error) {
    
  }
}

// 推荐数据加载完成
function handleRecommendationsLoaded(count) {
  recommendationCount.value = count
}

// 从推荐页面开始复习
function handleStartReviewFromRecommendations(data) {
  // 跳转到错题详情视图，进入复习模式
  currentMistakeId.value = data.mistakeId
  currentQuestionId.value = data.questionId
  // 使用 'review' 模式，初始隐藏答案，需要用户提交
  activeView.value = 'mistake-review'
}

// 处理复习提交完成
function handleReviewSubmitted(questionId) {
  // 刷新错题数量统计
  loadMistakeCount()
  loadRecommendationCount()
}

// 处理复习关闭（下一题或完成）
function handleReviewClosed() {
  // 清空当前题目，返回推荐列表
  currentMistakeId.value = null
  currentQuestionId.value = null
  activeView.value = 'recommendations'
}

onMounted(() => {
  loadMistakeCount()
  loadRecommendationCount()
})
</script>

<template>
  <div class="app-root">
    <!-- 顶部导航 -->
    <div class="top-nav-container">
       <div class="nav-left">
         <el-radio-group v-model="activeView" class="nav-radio-group">
           <!-- 智能推荐（默认） -->
           <el-radio-button value="recommendations">
             智能推荐
             <span v-if="recommendationCount > 0" class="recommendation-badge">
               ({{ recommendationCount }})
             </span>
           </el-radio-button>

           <el-radio-button value="history">习题历史</el-radio-button>

           <el-radio-button value="mistake">
             错题本
             <span v-if="mistakeCount > 0" class="mistake-badge-text">({{ mistakeCount }})</span>
           </el-radio-button>
         </el-radio-group>
       </div>
    </div>

    <!-- 主内容区 -->
    <main class="content-area">
      <Transition name="fade" mode="out-in">
        <!-- 智能推荐视图 -->
        <div v-if="activeView === 'recommendations'" key="recommendations" class="view-container">
          <RecommendationsPage
            @recommendations-loaded="handleRecommendationsLoaded"
            @start-review="handleStartReviewFromRecommendations"
          />
        </div>

        <!-- 习题历史 (Split View) -->
        <div v-else-if="activeView === 'history'" key="history" class="view-container split-layout">
           <!-- 左侧列表 -->
           <div class="list-pane glass-panel">
             <QuizHistoryPage :embedded="true" :selected-quiz-id="currentQuizId" @select-quiz="handleSelectQuiz" />
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

        <!-- 复习模式（独立组件） -->
        <div v-else-if="activeView === 'mistake-review'" key="mistake-review" class="view-container">
          <div class="review-pane-wrapper glass-panel">
            <ReviewModePane
              v-if="currentMistakeId"
              :key="currentQuestionId"
              :mistake-id="currentMistakeId"
              :question-id="currentQuestionId"
              @submitted="handleReviewSubmitted"
              @closed="handleReviewClosed"
            />
            <div v-else class="empty-detail-state">
              <div class="empty-icon-wrapper">
                <el-icon :size="48"><Document /></el-icon>
              </div>
              <h3>暂无复习题目</h3>
              <p>请从智能推荐中选择题目进行复习</p>
            </div>
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
  padding: 0 6px;
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

/* 推荐数量标记 */
.recommendation-badge {
  margin-left: 4px;
  color: #67c23a;
  font-weight: 600;
  font-size: 12px;
}

.nav-radio-group :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) .recommendation-badge {
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

  &.full-width {
    grid-column: 1 / -1;
  }
}

/* Adjust inner components to fit pane */
.detail-pane :deep(.quiz-detail-page) {
  /* 移除所有覆盖性设置，让组件保持自己的样式 */
  flex: 1;
  min-height: 0;
}

/* MistakeBookPage 已有自己的布局，不需要额外宽度设置 */

/* 复习模式容器 */
.review-pane-wrapper {
  height: 100%;
  overflow: hidden;
  border-radius: 16px;
}

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