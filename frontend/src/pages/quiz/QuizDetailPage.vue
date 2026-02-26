<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Collection } from '@element-plus/icons-vue'
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
    <!-- 加载状态 (骨架屏) -->
    <div v-if="loading" class="quiz-content-container">
      <el-skeleton animated style="height: 100%; display: flex; flex-direction: column;">
        <template #template>
          <!-- 顶部导航骨架 -->
          <div class="page-header">
            <el-skeleton-item v-if="!embedded" variant="text" style="width: 100px; margin-bottom: 12px;" />
            <el-skeleton-item variant="h1" style="width: 300px; height: 32px;" />
          </div>

          <!-- 统计信息骨架 -->
          <div class="statistics-bar">
            <div v-for="i in 5" :key="i" class="stat-item" style="margin-right: 24px;">
              <el-skeleton-item variant="text" style="width: 40px; margin-right: 8px;" />
              <el-skeleton-item variant="text" style="width: 60px;" />
            </div>
          </div>

          <!-- 题目列表骨架 -->
          <div class="questions-list-wrapper">
            <CustomScroll>
              <div class="questions-list">
                <div v-for="j in 2" :key="j" style="margin-bottom: 24px;">
                  <el-skeleton-item variant="text" style="width: 60px; margin-bottom: 16px;" />
                  <el-skeleton-item variant="p" style="width: 100%; margin-bottom: 8px;" />
                  <el-skeleton-item variant="p" style="width: 80%; margin-bottom: 24px;" />

                  <div v-for="k in 4" :key="k" style="margin-bottom: 12px; display: flex; align-items: center;">
                    <el-skeleton-item variant="circle" style="width: 20px; height: 20px; margin-right: 12px;" />
                    <el-skeleton-item variant="text" style="width: 60%;" />
                  </div>
                </div>
              </div>
            </CustomScroll>
          </div>
        </template>
      </el-skeleton>
    </div>

    <!-- 习题详情内容 -->
    <div v-else-if="quizDetail.quizId" class="quiz-content-container">
      <!-- 顶部导航 -->
      <div class="page-header">
        <div class="header-left">
          <div class="header-title">
            <el-icon class="title-icon"><Collection /></el-icon>
            <span class="title-text">{{ quizDetail.collectionName }}</span>
            <span class="subtitle-text">习题详情</span>
          </div>
        </div>

        <!-- 统计信息 (整合到 Header) -->
        <div class="header-right statistics-inline">
          <div class="stat-item">
            <span class="stat-label">正确率</span>
            <span class="stat-value">{{ accuracy }}%</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">正确数</span>
            <span class="stat-value">{{ correctCount }}/{{ totalCount }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">得分</span>
            <span class="stat-value">{{ quizDetail.totalScore || '-' }}</span>
          </div>
        </div>
      </div>

      <!-- 题目列表（只读模式） -->
      <div class="questions-list-wrapper">
        <CustomScroll>
          <div class="questions-list">
            <QuestionRenderer
              v-for="(question, index) in quizDetail.questions"
              :key="question.questionId"
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

/* Removed .quiz-detail-page.is-embedded .page-header override */

.quiz-detail-page.is-embedded .statistics-bar {
    /* Deprecated */
    display: none;
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
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 52px;
  min-height: 52px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
  transition: background 0.3s ease;

  html.dark & {
    background: rgba(30, 41, 59, 0.85);
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  font-size: 22px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 4px;
  border-radius: 6px;
  display: flex;
  transition: all 0.2s ease;
}

.title-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  letter-spacing: -0.3px;
}

.subtitle-text {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  padding-left: 12px;
  border-left: 1px solid var(--el-border-color);
  line-height: 1.2;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.statistics-inline {
  display: flex;
  align-items: center;
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  font-feature-settings: "tnum";
  font-variant-numeric: tabular-nums;
}

/* 响应式调整 */
@media (max-width: 900px) {
  .header-right {
    gap: 16px;
  }
  
  .time-item {
    display: none;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
  
  .header-right {
    width: 100%;
    justify-content: space-between;
  }
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
  gap: 16px;
  padding: 4px 24px 24px 24px;
}

/* ============ Dark Mode ============ */
html.dark .quiz-detail-page {
  .page-header {
    background: rgba(30, 41, 59, 0.7);
    border-color: rgba(255, 255, 255, 0.1);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);

    &.is-embedded {
      background: transparent;
      border-bottom-color: rgba(255, 255, 255, 0.1);
      box-shadow: none;
    }
  }

  .title-icon {
    background: rgba(64, 158, 255, 0.15);
  }

  .loading-container,
  .error-state {
    background: rgba(0, 0, 0, 0.2);

    .el-icon {
      color: var(--el-color-primary);
    }
  }

  .statistics-bar {
    /* Deprecated - kept for reference if needed but hidden */
    display: none;
  }

  .stat-label {
    color: rgba(255, 255, 255, 0.6);
  }

  .stat-value {
    color: #ffffff;
  }
}
</style>
