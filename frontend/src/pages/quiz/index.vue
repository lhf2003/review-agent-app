<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Document, ArrowLeft } from '@element-plus/icons-vue'
import QuizHistoryPage from './QuizHistoryPage.vue'
import QuizDetailPage from './QuizDetailPage.vue'
import MistakeListPane from '../../components/quiz/MistakeListPane.vue'
import MistakeDetailPane from '../../components/quiz/MistakeDetailPane.vue'
import ReviewModePane from '../../components/quiz/ReviewModePane.vue'
import RecommendationsPage from './RecommendationsPage.vue'
import { api } from '../../api/http'

const router = useRouter()
const activeView = ref('recommendations')
const currentQuizId = ref('')
const mistakeCount = ref(0)
const recommendationCount = ref(0)

// 错题本 split-layout 状态
const currentMistakeId = ref(null)
const currentQuestionId = ref(null)
const mistakeListPaneRef = ref(null)

function handleSelectQuiz(quizId) {
  currentQuizId.value = quizId
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
    // Silent fail
  }
}

async function loadRecommendationCount() {
  try {
    const stats = await api.getMistakeStats()
    recommendationCount.value = stats.unmastered || 0
  } catch (error) {
    // Silent fail
  }
}

function handleRecommendationsLoaded(count) {
  recommendationCount.value = count
}

function handleStartReviewFromRecommendations(data) {
  currentMistakeId.value = data.mistakeId
  currentQuestionId.value = data.questionId
  activeView.value = 'mistake-review'
}

function handleReviewSubmitted(questionId) {
  loadMistakeCount()
  loadRecommendationCount()
}

function handleReviewClosed() {
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
    <!-- Top Navigation -->
    <div class="top-nav-container">
      <!-- Left: Back Button -->
      <div class="nav-left">
        <div class="back-link" @click="$router.push('/dashboard')">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回</span>
        </div>
      </div>

      <!-- Center: Navigation Group -->
      <div class="nav-center">
        <el-radio-group v-model="activeView" class="nav-radio-group">
          <el-radio-button value="recommendations">
            智能推荐
            <span v-if="recommendationCount > 0" class="recommendation-badge">
              ({{ recommendationCount }})
            </span>
          </el-radio-button>

          <el-radio-button value="history">答题历史</el-radio-button>

          <el-radio-button value="mistake">
            错题本
            <span v-if="mistakeCount > 0" class="mistake-badge-text">({{ mistakeCount }})</span>
          </el-radio-button>
        </el-radio-group>
      </div>

    </div>

    <!-- Main Content Area -->
    <main class="content-area">
      <Transition name="fade" mode="out-in">
        <!-- Smart Recommendations -->
        <div v-if="activeView === 'recommendations'" key="recommendations" class="view-container">
          <RecommendationsPage
            @recommendations-loaded="handleRecommendationsLoaded"
            @start-review="handleStartReviewFromRecommendations"
          />
        </div>

        <!-- Quiz History -->
        <div v-else-if="activeView === 'history'" key="history" class="view-container split-layout">
          <div class="list-pane glass">
            <QuizHistoryPage 
              :embedded="true" 
              :selected-quiz-id="currentQuizId" 
              @select-quiz="handleSelectQuiz" 
            />
          </div>
          
          <div class="detail-pane glass">
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
                <h3>选择测验</h3>
                <p>点击列表中的测验记录查看详细分析</p>
              </div>
            </Transition>
          </div>
        </div>

        <!-- Mistake Book -->
        <div v-else-if="activeView === 'mistake'" key="mistake" class="view-container split-layout">
          <div class="list-pane glass">
            <MistakeListPane
              ref="mistakeListPaneRef"
              :embedded="true"
              @select-mistake="handleSelectMistake"
            />
          </div>

          <div class="detail-pane glass">
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
                <h3>选择错题</h3>
                <p>点击列表中的错题查看详细分析和掌握状态</p>
              </div>
            </Transition>
          </div>
        </div>

        <!-- Review Mode -->
        <div v-else-if="activeView === 'mistake-review'" key="mistake-review" class="view-container">
          <div class="review-pane-wrapper glass">
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
              <p>请从智能推荐中选择题目开始复习</p>
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

/* ============ Top Navigation ============ */
.top-nav-container {
  padding: 4px 16px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  z-index: 10;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.nav-center {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  padding: 8px 14px;
  border-radius: 999px;
  background: transparent;
  border: 1px solid transparent;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
}

.back-link:hover {
  color: var(--text-primary);
  background: var(--glass-surface);
  border-color: var(--glass-border);
  transform: translateY(-1px);
}

.back-link:active {
  transform: translateY(0);
}

.back-link .el-icon {
  font-size: 16px;
  transition: transform 0.3s ease;
}

.back-link:hover .el-icon {
  transform: translateX(-2px);
}

.nav-radio-group {
  background: var(--glass-surface);
  border: 1px solid var(--glass-border);
  border-radius: 12px;
  padding: 4px;
  display: flex;
  flex-wrap: nowrap;
  white-space: nowrap;
  transition: border-color 0.3s ease;
}

.nav-radio-group:hover {
  border-color: var(--glass-border-hover);
}

.nav-radio-group :deep(.el-radio-button__inner) {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  padding: 8px 18px;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  white-space: nowrap;
}

.nav-radio-group :deep(.el-radio-button__inner:hover) {
  background: rgba(255, 248, 245, 0.06);
  color: var(--text-primary);
}

.nav-radio-group :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: var(--accent-primary);
  color: white;
  box-shadow:
    0 4px 12px var(--accent-glow-soft),
    0 0 0 1px rgba(204, 102, 51, 0.3);
  font-weight: 600;
}

/* ============ Badge Styles ============ */
.mistake-badge-text {
  margin-left: 6px;
  color: var(--mastery-low);
  font-weight: 600;
  font-size: 12px;
  opacity: 0.9;
  transition: opacity 0.2s ease;
}

.recommendation-badge {
  margin-left: 6px;
  color: var(--mastery-high);
  font-weight: 600;
  font-size: 12px;
  opacity: 0.9;
  transition: opacity 0.2s ease;
}

/* Active 状态 badge 更亮 */
.nav-radio-group :deep(.el-radio-button.is-active .el-radio-button__inner) .mistake-badge-text,
.nav-radio-group :deep(.el-radio-button.is-active .el-radio-button__inner) .recommendation-badge {
  color: rgba(255, 255, 255, 0.95);
  opacity: 1;
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
  background: var(--bg-deep);
}

/* ============ Split Layout ============ */
.split-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 16px;
  height: 100%;
  overflow: visible;
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
  overflow: hidden;
  border-radius: 16px;
  position: relative;
  display: flex;
  flex-direction: column;
}

/* ============ Glass Panel ============ */
.glass {
  background: var(--glass-surface);
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
  border: 1px solid var(--glass-border);
  border-top: 1px solid var(--glass-highlight);
}

/* ============ Review Mode Container ============ */
.review-pane-wrapper {
  height: 100%;
  overflow: hidden;
  border-radius: 16px;
}

/* ============ Empty State ============ */
.empty-detail-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  text-align: center;
  padding: 40px;
}

.empty-icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255, 248, 245, 0.03);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  color: var(--text-tertiary);
}

.empty-detail-state h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--text-primary);
}

.empty-detail-state p {
  font-size: 14px;
  margin: 0;
  max-width: 300px;
  color: var(--text-tertiary);
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
    grid-template-columns: 1fr;
  }

  .top-nav-container {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    height: auto;
    padding: 12px 16px;
  }

  .nav-left {
    justify-content: center;
  }

  .nav-center {
    justify-content: center;
  }

  .nav-radio-group {
    display: flex;
    width: 100%;
  }

  .nav-radio-group :deep(.el-radio-button) {
    flex: 1;
  }

  .nav-radio-group :deep(.el-radio-button__inner) {
    padding: 8px 12px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .app-root {
    padding: 0;
  }

  .split-layout {
    gap: 8px;
    padding: 0;
  }
}
</style>
