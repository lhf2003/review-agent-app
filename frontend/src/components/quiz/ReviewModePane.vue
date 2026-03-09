<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, ArrowLeft } from '@element-plus/icons-vue'
import QuestionRenderer from './QuestionRenderer.vue'
import CustomScroll from '../CustomScroll.vue'
import { api } from '../../api/http'

/**
 * 复习模式面板组件
 * 独立于错题本，专门用于复习推荐流程
 */
const props = defineProps({
  mistakeId: {
    type: Number,
    required: true
  },
  questionId: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['submitted', 'closed'])

// 数据状态
const loading = ref(false)
const submitting = ref(false)
const questionDetail = ref(null)

// 复习模式状态
const currentAnswer = ref('')
const isSubmitted = ref(false)
const submitResult = ref(null)

// 加载题目详情
async function loadQuestionDetail() {
  if (!props.questionId) return

  loading.value = true
  try {
    const mistakeList = await api.getMistakeList('all')
    const target = mistakeList.find(m => m.questionId === props.questionId)

    if (target) {
      questionDetail.value = target
    }
  } catch (e) {
    ElMessage.error('加载题目详情失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

// 处理答案选择
function handleAnswerSelected(answer) {
  if (isSubmitted.value) return

  let actualAnswer = answer
  if (answer && typeof answer === 'object' && answer.letter) {
    actualAnswer = answer.letter
  }

  currentAnswer.value = actualAnswer
}

// 提交答案
async function submitAnswer() {
  if (!currentAnswer.value || !props.questionId) {
    ElMessage.warning('请先选择答案')
    return
  }

  submitting.value = true
  try {
    const result = await api.submitAnswer(props.questionId, currentAnswer.value, true)

    if (!result) {
      ElMessage.error('提交失败: 服务器返回空数据')
      return
    }

    submitResult.value = result
    isSubmitted.value = true

    const isCorrect = result.correct === true || result.isCorrect === true
    if (isCorrect) {
      ElMessage.success('回答正确！')
    } else {
      ElMessage.error('回答错误')
    }

    emit('submitted', props.questionId)
  } catch (error) {
    ElMessage.error('提交失败: ' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

// 返回
function handleBack() {
  emit('closed')
}

// 解析选项JSON
function parseOptions(optionsJson) {
  if (!optionsJson) return []
  try {
    return JSON.parse(optionsJson)
  } catch (e) {
    return []
  }
}

// 获取题目类型
function getQuestionType(questionType) {
  if (typeof questionType === 'string') {
    return questionType.toLowerCase()
  }
  return questionType?.name?.toLowerCase() || 'single_choice'
}

// 监听题目变化
watch(() => props.questionId, (newId) => {
  if (newId) {
    currentAnswer.value = ''
    isSubmitted.value = false
    submitResult.value = null
    loadQuestionDetail()
  }
}, { immediate: true })
</script>

<template>
  <div class="review-mode-pane">
    <!-- 顶部导航栏 -->
    <div class="review-header">
      <div class="header-title">
        <el-icon class="title-icon"><Document /></el-icon>
        <span>错题复习</span>
        <el-tag type="warning" size="small" effect="light" class="mode-tag">
          复习模式
        </el-tag>
      </div>
      <div class="header-actions">
        <el-button
          type="info"
          plain
          @click="handleBack"
          size="small"
        >
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-skeleton animated />
    </div>

    <!-- 空状态 -->
    <div v-else-if="!questionDetail" class="empty-state">
      <el-empty description="题目数据不存在" />
    </div>

    <!-- 题目内容 -->
    <CustomScroll v-else class="review-content">
      <QuestionRenderer
        :question="questionDetail.questionText"
        :type="getQuestionType(questionDetail.questionType)"
        :options="parseOptions(questionDetail.optionsJson)"
        :user-answer="isSubmitted ? (submitResult?.userAnswer || questionDetail.userAnswer) : currentAnswer"
        :correct-answer="questionDetail.correctAnswer"
        :explanation="questionDetail.explanation"
        :is-submitted="isSubmitted"
        :question-id="questionDetail.questionId"
        :knowledge-point="questionDetail.knowledgePoint"
        :blank-count="questionDetail.blankCount"
        @answer-selected="handleAnswerSelected"
      />

      <!-- 提交按钮区域 -->
      <div v-if="!isSubmitted" class="submit-section">
        <el-button
          type="primary"
          size="large"
          :disabled="!currentAnswer"
          :loading="submitting"
          @click="submitAnswer"
          class="submit-btn"
        >
          <span>提交答案</span>
          <el-icon><Right /></el-icon>
        </el-button>
        <p v-if="!currentAnswer" class="submit-hint">请先选择/输入答案</p>
      </div>

    </CustomScroll>
  </div>
</template>

<style scoped lang="scss">
.review-mode-pane {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--el-bg-color-page);
}

// 顶部导航栏
.review-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);

  .title-icon {
    font-size: 26px;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    padding: 6px;
    border-radius: 8px;
  }
}

.mode-tag {
  margin-left: 8px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

// 加载和空状态
.loading-state,
.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

// 内容区域
.review-content {
  flex: 1;
  padding: 20px;
}

// 提交按钮区域
.submit-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px;
  margin-top: 16px;
  background: rgba(64, 158, 255, 0.05);
  border-radius: 16px;
  border: 2px dashed rgba(64, 158, 255, 0.2);

  .submit-btn {
    min-width: 160px;
    height: 44px;
    font-size: 15px;
    font-weight: 600;
    border-radius: 22px;

    &:disabled {
      opacity: 0.6;
    }
  }

  .submit-hint {
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

</style>
