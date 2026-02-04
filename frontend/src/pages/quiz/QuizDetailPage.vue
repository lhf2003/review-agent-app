<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { api } from '../../api/http'
import QuestionRenderer from '../../components/quiz/QuestionRenderer.vue'
import CustomScroll from '../../components/CustomScroll.vue'

const route = useRoute()
const router = useRouter()

const props = defineProps({
  quizId: {
    type: String,
    default: ''
  },
  embedded: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['go-back'])

const quizDetail = ref({})
const loading = ref(false)

/**
 * 加载习题详情
 */
async function loadDetail() {
  loading.value = true
  try {
    const quizId = props.quizId || route.params.quizId
    if (!quizId) return
    quizDetail.value = await api.getQuizDetail(quizId)
  } catch (error) {
    ElMessage.error('加载习题详情失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

/**
 * 返回习题历史
 */
function goBack() {
  if (props.quizId) {
    emit('go-back')
  } else {
    router.push('/quiz-history')
  }
}

/**
 * 解析选项JSON字符串为数组
 */
function parseOptions(optionsJson) {
  if (!optionsJson) return []
  try {
    return JSON.parse(optionsJson)
  } catch (e) {
    console.error('解析选项失败:', e)
    return []
  }
}

/**
 * 获取题目类型字符串（从枚举转换）
 */
function getQuestionType(questionType) {
  if (typeof questionType === 'string') {
    return questionType
  }
  // 如果是枚举对象，获取其name值
  return questionType?.name || 'single_choice'
}

/**
 * 计算统计数据
 */
const correctCount = computed(() => {
  if (!quizDetail.value.questions) return 0
  return quizDetail.value.questions.filter(q => q.isCorrect === true).length
})

const totalCount = computed(() => {
  return quizDetail.value.questions?.length || 0
})

const accuracy = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round((correctCount.value / totalCount.value) * 100)
})

watch(() => props.quizId, (newId) => {
  if (newId) {
    loadDetail()
  }
})

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <div class="quiz-detail-page" :class="{ 'is-embedded': embedded }">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <!-- 习题详情内容 -->
    <div v-else-if="quizDetail.quizId" class="quiz-content-container">
      <!-- 顶部导航 -->
      <div class="page-header" :class="{ 'is-embedded': embedded }">
        <el-button v-if="!embedded" @click="goBack" link>
          <el-icon><ArrowLeft /></el-icon>
          返回习题历史
        </el-button>
        <h1>{{ quizDetail.collectionName }} - 习题详情</h1>
      </div>

      <!-- 统计信息 -->
      <div class="statistics-bar">
        <div class="stat-item">
          <span class="stat-label">正确率:</span>
          <span class="stat-value">{{ accuracy }}%</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">正确数:</span>
          <span class="stat-value">{{ correctCount }}/{{ totalCount }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">得分:</span>
          <span class="stat-value">{{ quizDetail.totalScore || '-' }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">状态:</span>
          <span class="stat-value">
            <el-tag v-if="quizDetail.isOutdated" type="warning">已过期</el-tag>
            <el-tag v-else-if="quizDetail.status === 1" type="success">已完成</el-tag>
            <el-tag v-else type="info">进行中</el-tag>
          </span>
        </div>
        <div class="stat-item">
          <span class="stat-label">完成时间:</span>
          <span class="stat-value">{{ quizDetail.createdTime }}</span>
        </div>
      </div>

      <!-- 题目列表（只读模式） -->
      <div class="questions-list-wrapper">
        <CustomScroll>
          <div class="questions-list">
            <div
              v-for="(question, index) in quizDetail.questions"
              :key="question.questionId"
              class="question-review"
            >
              <div class="question-number">第 {{ index + 1 }} 题</div>
              <QuestionRenderer
                :question="question.questionText"
                :type="getQuestionType(question.questionType)"
                :options="parseOptions(question.optionsJson)"
                :user-answer="question.userAnswer"
                :correct-answer="question.correctAnswer"
                :explanation="question.explanation"
                :is-submitted="true"
                :question-id="question.questionId"
                :index="index + 1"
                :knowledge-point="question.knowledgePoint"
                :blank-count="question.blankCount"
              />
            </div>
          </div>
        </CustomScroll>
      </div>
    </div>

    <!-- 错误状态 -->
    <div v-else class="error-state">
      <el-empty description="习题不存在或无权访问">
        <el-button type="primary" @click="goBack">返回</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script>
import { Loading } from '@element-plus/icons-vue'
export default {
  components: { Loading }
}
</script>

<style scoped>
.quiz-detail-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden; /* Ensure it doesn't overflow parent */
}

.quiz-detail-page * {
  box-sizing: border-box; /* Prevent padding from affecting width */
}

.quiz-content-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  width: 100%;
}

/* Embedded mode adjustments */
.quiz-detail-page.is-embedded {
  padding: 0;
  max-width: none;
}

.quiz-detail-page.is-embedded .page-header {
  padding: 24px 24px 0 24px;
  margin-bottom: 24px;
}

.quiz-detail-page.is-embedded .statistics-bar {
  margin: 0 24px 24px 24px;
}

.quiz-detail-page.is-embedded .questions-list {
  padding: 4px 24px 24px 24px;
}

.loading-container,
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--el-text-color-secondary);
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 12px 0 0 0;
  color: var(--el-text-color-primary);
}

.page-header.is-embedded h1 {
  margin-top: 0;
}

.statistics-bar {
  display: flex;
  gap: 24px;
  padding: 20px;
  background: var(--el-fill-color);
  border-radius: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.questions-list-wrapper {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

.questions-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 4px;
}

.question-review {
  padding: 24px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color);
  border-radius: 12px;
}

.question-number {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin-bottom: 16px;
}

/* ============ Dark Mode ============ */
html.dark .quiz-detail-page {
  .loading-container,
  .error-state {
    background: rgba(0, 0, 0, 0.2);

    .el-icon {
      color: var(--el-color-primary);
    }
  }

  .statistics-bar {
    background: rgba(28, 28, 30, 0.75);
    backdrop-filter: blur(20px) saturate(180%);
    border: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
  }

  .question-review {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);

    &:hover {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.15);
    }
  }

  .question-number {
    color: rgba(255, 255, 255, 0.7);
  }

  .stat-label {
    color: rgba(255, 255, 255, 0.6);
  }

  .stat-value {
    color: #ffffff;
  }
}
</style>
