<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, RefreshRight, ArrowRight, Aim, Loading, InfoFilled, SuccessFilled } from '@element-plus/icons-vue'
import { api } from '../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import QuestionRenderer from '../components/quiz/QuestionRenderer.vue'

const route = useRoute()
const router = useRouter()
const collectionId = route.params.id

const loading = ref(false)
const collectionInfo = ref({})
const quizQuestions = ref([])
const quizId = ref(null)

// 当前答题状态
const currentQuestionIndex = ref(0)
const showAnswer = ref(false)
const currentUserAnswer = ref(null)
// 本地答案存储（key: questionId, value: userAnswer）
const answersMap = ref(new Map())
// 是否已全部提交
const isAllSubmitted = ref(false)
// 答题结果统计
const quizResult = ref(null)
// 是否显示结果弹窗
const showResultDialog = ref(false)

onMounted(async () => {
  await fetchCollectionInfo()
  await startQuiz()
})

async function fetchCollectionInfo() {
  try {
    const res = await api.getCollectionDetail(collectionId)
    collectionInfo.value = res
  } catch (e) {
    ElMessage.error('获取合集信息失败')
  }
}

async function startQuiz() {
  loading.value = true
  try {
    // 检查合集是否有内容
    if (collectionInfo.value.analysisResults && collectionInfo.value.analysisResults.length === 0) {
      ElMessage.warning('合集为空，无法开始学习')
      setTimeout(() => router.back(), 1500)
      return
    }

    const res = await api.generateQuiz(collectionId)
    quizId.value = res.id
    quizQuestions.value = res.questions || []
    currentQuestionIndex.value = 0
    loadQuestionState(0)

    if (!quizQuestions.value || quizQuestions.value.length === 0) {
      ElMessage.warning('未能生成题目，请确保合集有已分析的内容')
    }
  } catch (e) {
    console.error('生成题目失败:', e)
    ElMessage.error(`生成题目失败: ${e.message || '未知错误'}`)
  } finally {
    loading.value = false
  }
}

function loadQuestionState(index) {
  const q = quizQuestions.value[index]
  if (q && q.userAnswer) {
    // 已提交的答案（来自后端）
    currentUserAnswer.value = q.userAnswer
    showAnswer.value = true
  } else if (q && answersMap.value.has(q.id)) {
    // 未提交的本地答案
    currentUserAnswer.value = answersMap.value.get(q.id)
    showAnswer.value = isAllSubmitted.value // 仅在全部提交后显示解析
  } else {
    currentUserAnswer.value = null
    showAnswer.value = isAllSubmitted.value
  }
}

function handleQuestionAnswer(answer) {
  // 如果已全部提交，不允许修改
  if (isAllSubmitted.value) return

  const q = quizQuestions.value[currentQuestionIndex.value]

  // 处理 answer 格式
  let actualAnswer = answer
  if (answer && typeof answer === 'object' && answer.letter) {
    actualAnswer = answer.letter
  }

  // 存储到本地 Map，暂不提交到后端
  answersMap.value.set(q.id, actualAnswer)
  currentUserAnswer.value = actualAnswer
}

async function submitAllAnswers() {
  if (isAllSubmitted.value) return

  try {
    // 1. 检查是否有未作答的题目
    const unansweredQuestions = []
    for (let i = 0; i < quizQuestions.value.length; i++) {
      const q = quizQuestions.value[i]
      if (!answersMap.value.has(q.id)) {
        unansweredQuestions.push(i + 1) // 题号从1开始
      }
    }

    // 2. 如果有未作答的题目，提示用户
    if (unansweredQuestions.length > 0) {
      const questionNumbers = unansweredQuestions.join('、')
      try {
        await ElMessageBox.confirm(
          `第 ${questionNumbers} 题还没有作答，确定要提交吗？`,
          '提示',
          {
            confirmButtonText: '确定提交',
            cancelButtonText: '继续答题',
            type: 'warning'
          }
        )
      } catch {
        return // 用户选择继续答题，不提交
      }
    }

    ElMessage.info('正在提交答案...')

    // 3. 转换为批量提交格式
    const batchAnswers = quizQuestions.value.map(q => ({
      questionId: q.id,
      userAnswer: answersMap.value.get(q.id) || null
    }))

    // 4. 调用批量提交接口
    const result = await api.submitBatchAnswers(quizId.value, batchAnswers)

    // 5. 保存结果统计
    quizResult.value = result

    // 6. 更新本地状态
    quizQuestions.value.forEach((q, index) => {
      const answer = batchAnswers[index].userAnswer
      if (answer) {
        q.userAnswer = answer
      }
    })

    // 7. 设置提交状态
    isAllSubmitted.value = true
    showAnswer.value = true

    // 8. 显示结果弹窗
    showResultDialog.value = true

  } catch (e) {
    console.error('提交答案失败:', e)

    // 根据错误类型显示不同提示
    if (e.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      setTimeout(() => router.push('/login'), 1500)
    } else if (e.response?.status === 404) {
      ElMessage.error('测验记录不存在，请重新开始')
      setTimeout(() => router.back(), 1500)
    } else if (e.code === 'ECONNABORTED') {
      ElMessage.error('网络超时，请检查网络连接后重试')
    } else {
      ElMessage.error(`提交失败: ${e.message || '未知错误'}`)
    }
  }
}

async function handleResetQuiz() {
  try {
    await ElMessageBox.confirm('确定要重新开始测验吗？这将清除当前的答题进度。', '重新开始', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    loading.value = true
    await api.resetQuiz(quizId.value)
    ElMessage.success('答题记录已重置')

    // 清空所有答案状态
    quizQuestions.value.forEach(q => {
      q.userAnswer = null
    })
    answersMap.value.clear()
    isAllSubmitted.value = false
    currentQuestionIndex.value = 0
    loadQuestionState(0)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('重置失败')
    }
  } finally {
    loading.value = false
  }
}

function nextQuestion() {
  if (currentQuestionIndex.value < quizQuestions.value.length - 1) {
    currentQuestionIndex.value++
    loadQuestionState(currentQuestionIndex.value)
  }
}

function prevQuestion() {
  if (currentQuestionIndex.value > 0) {
    currentQuestionIndex.value--
    loadQuestionState(currentQuestionIndex.value)
  }
}

function closeResultDialog() {
  showResultDialog.value = false
  // 用户可以继续查看题目解析
}

function getAccuracyColor(ratio) {
  if (ratio >= 0.8) return '#67c23a'      // 绿色
  if (ratio >= 0.6) return '#e6a23c'      // 橙色
  return '#f56c6c'                         // 红色
}

const progressPercentage = computed(() => {
  if (!quizQuestions.value.length) return 0
  return ((currentQuestionIndex.value + 1) / quizQuestions.value.length) * 100
})
</script>

<template>
  <div class="quiz-page">
    <!-- 顶部导航 -->
    <div class="nav-header">
      <div class="header-left">
        <el-button 
          link 
          class="back-btn" 
          @click="router.back()"
        >
          <el-icon><ArrowLeft /></el-icon>
          <span class="back-text">返回合集</span>
        </el-button>
        <div class="divider"></div>
        <h1 class="header-title">AI 学习辅导</h1>
        <span class="collection-name" v-if="collectionInfo.name">
          {{ collectionInfo.name }}
        </span>
      </div>
      
      <div class="header-right">
        <el-button 
          type="primary" 
          plain
          round
          :icon="RefreshRight" 
          @click="handleResetQuiz"
          class="reset-btn"
        >
          重新开始
        </el-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="quiz-content" v-loading="loading">
      <!-- 右侧垂直进度指示器 -->
      <div class="vertical-progress" v-if="quizQuestions.length > 0">
        <div class="progress-indicator">
          <div class="progress-number">{{ currentQuestionIndex + 1 }}</div>
          <div class="progress-divider">/</div>
          <div class="progress-total">{{ quizQuestions.length }}</div>
        </div>
        <div class="progress-bar-vertical">
          <div
            class="progress-fill-vertical"
            :style="{ height: `${progressPercentage}%` }"
          ></div>
        </div>
      </div>

      <div v-if="quizQuestions.length > 0" class="quiz-container">
        <!-- 题目渲染区域 -->
        <div class="question-wrapper">
          <transition name="fade-slide" mode="out-in">
            <div :key="currentQuestionIndex" class="question-card-container">
              <QuestionRenderer
                v-if="quizQuestions[currentQuestionIndex]"
                :question-id="quizQuestions[currentQuestionIndex].id"
                :question="quizQuestions[currentQuestionIndex].question"
                :type="quizQuestions[currentQuestionIndex].type || 'single_choice'"
                :options="quizQuestions[currentQuestionIndex].options || []"
                :correct-answer="quizQuestions[currentQuestionIndex].answer"
                :explanation="quizQuestions[currentQuestionIndex].explanation"
                :user-answer="currentUserAnswer"
                :is-submitted="showAnswer"
                :knowledge-point="quizQuestions[currentQuestionIndex].knowledgePoint"
                :blank-count="quizQuestions[currentQuestionIndex].blankCount"
                @answer-selected="handleQuestionAnswer"
              />
            </div>
          </transition>
        </div>

        <!-- 底部控制栏 -->
        <div class="quiz-footer">
          <el-button
            :disabled="currentQuestionIndex === 0"
            @click="prevQuestion"
            size="large"
            class="nav-btn prev-btn"
            :icon="ArrowLeft"
            round
          >
            上一题
          </el-button>

          <!-- 提交答案按钮（仅在最后一题且未提交时显示） -->
          <el-button
            v-if="currentQuestionIndex === quizQuestions.length - 1 && !isAllSubmitted"
            type="success"
            @click="submitAllAnswers"
            size="large"
            class="nav-btn submit-btn"
            round
          >
            <el-icon class="el-icon--left"><Aim /></el-icon>
            提交答案
          </el-button>

          <el-button
            v-if="!isAllSubmitted"
            type="primary"
            :disabled="currentQuestionIndex === quizQuestions.length - 1"
            @click="nextQuestion"
            size="large"
            class="nav-btn next-btn"
            round
          >
            下一题
            <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>

          <!-- 已提交后的完成提示 -->
          <div v-if="isAllSubmitted" class="completion-badge">
            <el-icon color="#67c23a"><SuccessFilled /></el-icon>
            <span>已完成答题</span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loading" class="empty-state">
        <el-empty description="未能生成练习题，请稍后再试" />
        <el-button type="primary" @click="router.back()">返回合集</el-button>
      </div>
    </div>

    <!-- 答题结果弹窗 -->
    <el-dialog
      v-model="showResultDialog"
      title="📊 答题结果"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="true"
      @close="closeResultDialog"
    >
      <div class="quiz-result-dialog">
        <!-- 统计卡片 -->
        <div class="result-summary">
          <div class="summary-item total">
            <div class="label">总题数</div>
            <div class="value">{{ quizResult?.totalCount || 0 }}</div>
            <div class="unit">题</div>
          </div>

          <div class="summary-item correct">
            <div class="label">✅ 正确</div>
            <div class="value">{{ quizResult?.correctCount || 0 }}</div>
            <div class="unit">题</div>
          </div>

          <div class="summary-item incorrect">
            <div class="label">❌ 错误</div>
            <div class="value">{{ quizResult?.incorrectCount || 0 }}</div>
            <div class="unit">题</div>
          </div>

          <div class="summary-item unanswered" v-if="quizResult?.unansweredCount > 0">
            <div class="label">⏭️ 未作答</div>
            <div class="value">{{ quizResult?.unansweredCount || 0 }}</div>
            <div class="unit">题</div>
          </div>
        </div>

        <!-- 正确率进度条 -->
        <div class="accuracy-section" v-if="quizResult?.totalCount > 0">
          <div class="accuracy-label">正确率</div>
          <div class="accuracy-bar">
            <el-progress
              :percentage="Math.round((quizResult.correctCount / quizResult.totalCount) * 100)"
              :color="getAccuracyColor(quizResult.correctCount / quizResult.totalCount)"
              :stroke-width="20"
              :show-text="true"
            />
          </div>
        </div>

        <!-- 提示信息 -->
        <div class="result-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>点击下方按钮返回查看各题详细解析</span>
        </div>
      </div>

      <template #footer>
        <el-button type="primary" size="large" @click="closeResultDialog" style="width: 100%">
          查看详细解析
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.quiz-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color-page);
  color: var(--el-text-color-primary);
  overflow: hidden; // Prevent page-level scroll
}

.nav-header {
  height: 64px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--el-bg-color-page);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
  z-index: 100;
  
  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    
    .back-btn {
      font-size: 15px;
      font-weight: 500;
      color: var(--el-text-color-regular);
      padding: 0;
      height: auto;
      
      &:hover {
        color: var(--el-color-primary);
      }
      
      .el-icon {
        margin-right: 4px;
        font-size: 18px;
      }
    }
    
    .divider {
      width: 1px;
      height: 24px;
      background-color: var(--el-border-color-light);
    }
    
    .header-title {
      font-size: 18px;
      font-weight: 700;
      margin: 0;
      color: var(--el-text-color-primary);
    }
    
    .collection-name {
      font-size: 14px;
      color: var(--el-text-color-secondary);
      background: var(--el-fill-color-light);
      padding: 4px 12px;
      border-radius: 12px;
    }
  }
}

.quiz-content {
  flex: 1;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px); // Subtract header height
}

// 右侧垂直进度指示器
.vertical-progress {
  position: fixed;
  right: 32px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 50;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;

  .progress-indicator {
    display: flex;
    align-items: baseline;
    gap: 4px;
    font-weight: 700;
    font-size: 24px;
    color: var(--el-text-color-primary);
    font-variant-numeric: tabular-nums;

    .progress-number {
      font-size: 32px;
      color: var(--el-color-primary);
    }

    .progress-divider {
      font-size: 20px;
      color: var(--el-text-color-secondary);
      opacity: 0.6;
    }

    .progress-total {
      font-size: 18px;
      color: var(--el-text-color-secondary);
      opacity: 0.8;
    }
  }

  .progress-bar-vertical {
    width: 6px;
    height: 200px;
    background: var(--el-fill-color-darker);
    border-radius: 3px;
    overflow: hidden;
    position: relative;
  }

  .progress-fill-vertical {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: linear-gradient(180deg, var(--el-color-primary-light-3), var(--el-color-primary));
    border-radius: 3px;
    transition: height 0.5s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 0 10px rgba(var(--el-color-primary-rgb), 0.3);
  }
}

.quiz-container {
  width: 100%;
  height: 100%;
  margin: 0;
  display: flex;
  flex-direction: column;
  padding: 24px 80px 24px 48px; // Right padding accommodates progress bar
  box-sizing: border-box;
}

.question-wrapper {
  flex: 1;
  overflow-y: auto; // Internal scroll only
  padding-right: 16px;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;

  // Hide scrollbar visually but keep functionality
  scrollbar-width: none; // Firefox
  &::-webkit-scrollbar {
    display: none; // Chrome/Safari
  }

  .question-card-container {
    width: 100%;
    max-width: 1400px; // Increased from 900px to maximize space
    margin: 0 auto;
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center; // Center vertically if content is short
  }
}

.quiz-footer {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding: 20px 0 0 0;
  flex-shrink: 0; // Prevent footer from shrinking
  align-items: center;

  .nav-btn {
    min-width: 160px;
    height: 52px;
    font-size: 16px;
    font-weight: 600;
    border-radius: 26px; // Pill shape

    &.prev-btn {
      background: var(--el-bg-color);
      border: 1px solid var(--el-border-color);

      &:hover:not(:disabled) {
        background: var(--el-fill-color-light);
        border-color: var(--el-border-color-darker);
      }
    }

    &.next-btn {
      box-shadow: 0 4px 12px rgba(var(--el-color-primary-rgb), 0.3);

      &:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 8px 16px rgba(var(--el-color-primary-rgb), 0.4);
      }

      &:active:not(:disabled) {
        transform: scale(0.98);
      }
    }

    &.submit-btn {
      background: var(--el-color-success);
      border-color: var(--el-color-success);
      box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);

      &:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 8px 16px rgba(103, 194, 58, 0.4);
      }

      &:active:not(:disabled) {
        transform: scale(0.98);
      }
    }
  }

  .completion-badge {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 24px;
    background: rgba(103, 194, 58, 0.1);
    border: 1px solid var(--el-color-success);
    border-radius: 26px;
    font-size: 16px;
    font-weight: 600;
    color: var(--el-color-success);

    .el-icon {
      font-size: 20px;
    }
  }
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

// Transitions
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

// Dark Mode Adaptation
:global(.dark) {
  .nav-header {
    background: rgba(28, 28, 30, 0.75); // Modern Glass Dark
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .vertical-progress {
    .progress-indicator {
      color: var(--el-text-color-primary);
    }

    .progress-bar-vertical {
      background: rgba(255, 255, 255, 0.1);
    }
  }

  .quiz-footer .nav-btn.prev-btn {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: white;

    &:hover:not(:disabled) {
      background: rgba(255, 255, 255, 0.1);
    }
  }

  .quiz-footer .nav-btn.submit-btn {
    background: var(--el-color-success);
    color: white;
  }

  .quiz-footer .completion-badge {
    background: rgba(103, 194, 58, 0.15);
    border-color: rgba(103, 194, 58, 0.4);
    color: #67c23a;
  }
}

// Responsive Design
@media (max-width: 1024px) {
  .quiz-container {
    padding: 24px 32px;
  }

  .vertical-progress {
    right: 24px;
  }
}

@media (max-width: 768px) {
  .vertical-progress {
    right: 16px;

    .progress-indicator {
      font-size: 18px;

      .progress-number {
        font-size: 24px;
      }

      .progress-divider {
        font-size: 16px;
      }

      .progress-total {
        font-size: 14px;
      }
    }

    .progress-bar-vertical {
      height: 150px;
    }
  }

  .quiz-container {
    padding: 20px 24px;
    padding-right: 60px; // Space for vertical progress
  }

  .quiz-footer {
    .nav-btn {
      min-width: 120px;
      height: 48px;
      font-size: 15px;
    }
  }
}

@media (max-width: 480px) {
  .vertical-progress {
    right: 12px;
    gap: 12px;

    .progress-indicator {
      font-size: 16px;

      .progress-number {
        font-size: 20px;
      }

      .progress-divider {
        font-size: 14px;
      }

      .progress-total {
        font-size: 12px;
      }
    }

    .progress-bar-vertical {
      width: 4px;
      height: 120px;
    }
  }

  .quiz-container {
    padding: 16px 16px;
    padding-right: 48px; // Space for vertical progress
  }

  .quiz-footer {
    gap: 16px;

    .nav-btn {
      min-width: 100px;
      height: 44px;
      font-size: 14px;
      padding: 0 16px;
    }
  }

  .nav-header {
    padding: 0 16px;

    .header-left {
      gap: 12px;

      .back-text {
        display: none; // Hide text on very small screens
      }
    }

    .header-right {
      .reset-btn {
        padding: 8px 12px;
        font-size: 14px;
      }
    }
  }
}

// 答题结果弹窗样式
.quiz-result-dialog {
  padding: 20px 0;

  .result-summary {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    margin-bottom: 24px;

    .summary-item {
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
      backdrop-filter: blur(10px);
      border-radius: 16px;
      padding: 20px;
      text-align: center;
      border: 1px solid rgba(255, 255, 255, 0.18);
      transition: transform 0.3s cubic-bezier(0.25, 1, 0.5, 1);

      &:hover {
        transform: translateY(-4px);
      }

      &.total {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border: none;
      }

      &.correct {
        background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%);
        border: none;

        .label, .unit { color: #0f766e; }
        .value { color: #0f766e; font-weight: bold; }
      }

      &.incorrect {
        background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 99%, #fecfef 100%);
        border: none;

        .label, .unit { color: #dc2626; }
        .value { color: #dc2626; font-weight: bold; }
      }

      &.unanswered {
        background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
        border: none;

        .label, .unit { color: #4338ca; }
        .value { color: #4338ca; font-weight: bold; }
      }

      .label {
        font-size: 14px;
        margin-bottom: 8px;
        color: rgba(255, 255, 255, 0.9);
      }

      .value {
        font-size: 32px;
        font-weight: bold;
        color: white;
        line-height: 1;
        margin-bottom: 4px;
      }

      .unit {
        font-size: 12px;
        color: rgba(255, 255, 255, 0.7);
      }
    }
  }

  .accuracy-section {
    margin-bottom: 24px;

    .accuracy-label {
      font-size: 14px;
      color: rgba(255, 255, 255, 0.8);
      margin-bottom: 12px;
      text-align: center;
    }

    .accuracy-bar {
      padding: 0 20px;
    }
  }

  .result-tip {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 16px;
    background: rgba(103, 194, 58, 0.1);
    border-radius: 12px;
    border: 1px solid rgba(103, 194, 58, 0.2);
    color: #67c23a;
    font-size: 14px;

    .el-icon {
      font-size: 18px;
    }
  }
}
</style>