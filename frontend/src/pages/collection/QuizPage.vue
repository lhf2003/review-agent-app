<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../../api/http'
import { ElMessage, ElMessageBox } from 'element-plus'
import QuestionRenderer from '../../components/quiz/QuestionRenderer.vue'
import QuizNavHeader from '../../components/quiz/QuizNavHeader.vue'
import QuizProgressIndicator from '../../components/quiz/QuizProgressIndicator.vue'
import QuizFooter from '../../components/quiz/QuizFooter.vue'
import QuizResultModal from '../../components/quiz/QuizResultModal.vue'
import QuizLoadingModal from '../../components/quiz/QuizLoadingModal.vue'
import CustomScroll from '../../components/CustomScroll.vue'

const route = useRoute()
const router = useRouter()
const collectionId = route.params.id

const loading = ref(false)
const showLoadingModal = ref(false)
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
  showLoadingModal.value = true
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
    showLoadingModal.value = false
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
    ElMessage.success('提交成功')

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

const progressPercentage = computed(() => {
  if (!quizQuestions.value.length) return 0
  return ((currentQuestionIndex.value + 1) / quizQuestions.value.length) * 100
})
</script>

<template>
  <div class="quiz-page">
    <!-- 顶部导航 -->
    <QuizNavHeader
      :collection-name="collectionInfo.name"
      @back="router.back()"
      @reset="handleResetQuiz"
    />

    <!-- 主要内容区域 -->
    <div class="quiz-content" v-loading="loading">
      <!-- 右侧垂直进度指示器 -->
      <QuizProgressIndicator
        v-if="quizQuestions.length > 0"
        :current="currentQuestionIndex + 1"
        :total="quizQuestions.length"
        :percentage="progressPercentage"
      />

      <div v-if="quizQuestions.length > 0" class="quiz-container">
        <!-- 题目渲染区域 -->
        <CustomScroll class="question-wrapper" :hide-scrollbar="true">
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
                :index="currentQuestionIndex + 1"
                :knowledge-point="quizQuestions[currentQuestionIndex].knowledgePoint"
                :blank-count="quizQuestions[currentQuestionIndex].blankCount"
                @answer-selected="handleQuestionAnswer"
                @answer-changed="handleQuestionAnswer"
              />
            </div>
          </transition>
        </CustomScroll>

        <!-- 底部控制栏 -->
        <QuizFooter
          :current-index="currentQuestionIndex"
          :total-questions="quizQuestions.length"
          :is-all-submitted="isAllSubmitted"
          :can-go-prev="currentQuestionIndex > 0"
          :can-go-next="currentQuestionIndex < quizQuestions.length - 1"
          @prev="prevQuestion"
          @next="nextQuestion"
          @submit="submitAllAnswers"
        />
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loading" class="empty-state">
        <el-empty description="未能生成练习题，请稍后再试" />
        <el-button type="primary" @click="router.back()">返回合集</el-button>
      </div>
    </div>

    <!-- 答题结果弹窗 -->
    <QuizResultModal
      v-model="showResultDialog"
      :quiz-result="quizResult"
      @reset="handleResetQuiz"
    />

    <!-- AI 生成试题加载弹窗 -->
    <QuizLoadingModal
      v-model="showLoadingModal"
      :collection-name="collectionInfo.name"
    />
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

.quiz-content {
  flex: 1;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px); // Subtract header height
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
  padding-right: 16px;
  margin-bottom: 0;
  display: flex;
  flex-direction: column;

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

// Responsive Design
@media (max-width: 1024px) {
  .quiz-container {
    padding: 24px 32px;
  }
}

@media (max-width: 768px) {
  .quiz-container {
    padding: 20px 24px;
    padding-right: 60px; // Space for vertical progress
  }
}

@media (max-width: 480px) {
  .quiz-container {
    padding: 16px 16px;
    padding-right: 48px; // Space for vertical progress
  }
}
</style>