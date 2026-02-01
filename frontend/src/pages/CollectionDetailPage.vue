<script setup>
 import { ref, onMounted, onUnmounted, useTemplateRef, watch } from 'vue'
 import { useRoute, useRouter } from 'vue-router'
 import { ArrowLeft, Reading, Delete, FolderOpened, Aim, Timer, RefreshRight } from '@element-plus/icons-vue'
 import { api } from '../api/http'
 import { ElMessage, ElMessageBox } from 'element-plus'
 import AnimatedList from '../components/AnimatedList.vue'
 import QuestionRenderer from '../components/quiz/QuestionRenderer.vue'

const route = useRoute()
const router = useRouter()
const info = ref({})
const loading = ref(false)
const quizVisible = ref(false)
const quizQuestions = ref([])
const quizLoading = ref(false)
const quizId = ref(null)

// 当前答题状态
const currentQuestionIndex = ref(0)
const showAnswer = ref(false)
const currentUserAnswer = ref(null)

onMounted(() => {
  fetchDetail()
})

async function fetchDetail() {
  loading.value = true
  try {
    const res = await api.getCollectionDetail(route.params.id)
    info.value = res
  } finally {
    loading.value = false
  }
}

async function startLearning() {
  if (!info.value.analysisResults || info.value.analysisResults.length === 0) {
    ElMessage.warning('合集为空，无法开始学习')
    return
  }
  quizVisible.value = true
  quizLoading.value = true
  try {
    const res = await api.generateQuiz(info.value.id)
    quizId.value = res.id
    quizQuestions.value = res.questions || []
    currentQuestionIndex.value = 0
    loadQuestionState(0)

    // 检查是否成功生成题目
    if (!quizQuestions.value || quizQuestions.value.length === 0) {
      ElMessage.warning('未能生成题目，请确保合集有已分析的内容')
    }
  } catch(e) {
    console.error('生成题目失败，详细错误:', e)
    // 显示详细的错误信息
    const errorMsg = e.message || '未知错误'
    ElMessage.error(`生成题目失败: ${errorMsg}`)
  } finally {
    quizLoading.value = false
  }
}

function loadQuestionState(index) {
  const q = quizQuestions.value[index]
  if (q && q.userAnswer) {
    currentUserAnswer.value = q.userAnswer
    showAnswer.value = true
  } else {
    currentUserAnswer.value = null
    showAnswer.value = false
  }
}

async function handleQuestionAnswer(answer) {
  if (showAnswer.value) return // 已答题不可修改

  const q = quizQuestions.value[currentQuestionIndex.value]

  // Convert answer based on question type
  let formattedAnswer = answer
  if (q.type === 'multiple_choice') {
    // Multiple choice: array of option keys -> comma-separated string
    formattedAnswer = answer.join(',')
  } else if (q.type === 'fill_blank') {
    // Fill blank: object -> JSON string
    formattedAnswer = JSON.stringify(answer)
  } else if (q.type === 'code_snippet') {
    // Code snippet: number -> string
    formattedAnswer = String(answer)
  }

  currentUserAnswer.value = answer
  showAnswer.value = true
  q.userAnswer = answer // 本地更新状态

  try {
    await api.submitAnswer(q.id, formattedAnswer)
  } catch (e) {
    ElMessage.error('保存答案失败')
  }
}

async function handleResetQuiz() {
  try {
    await api.resetQuiz(quizId.value)
    ElMessage.success('答题记录已重置')
    // 清空本地状态
    quizQuestions.value.forEach(q => {
        q.userAnswer = null
    })
    loadQuestionState(currentQuestionIndex.value)
  } catch (e) {
    ElMessage.error('重置失败')
  }
}

function nextQuestion() {
    currentQuestionIndex.value++
    loadQuestionState(currentQuestionIndex.value)
}

function prevQuestion() {
    currentQuestionIndex.value--
    loadQuestionState(currentQuestionIndex.value)
}

function confirmRemove(item) {
  ElMessageBox.confirm(
    '确定要从合集中移除这个问题吗？',
    '移除确认',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(() => {
    removeSession(item.id)
  }).catch(() => {})
}

async function removeSession(sessionId) {
  try {
    await api.removeSessionFromCollection(info.value.id, sessionId)
    ElMessage.success('已移除')
    // 本地移除
    if (info.value.analysisResults) {
      info.value.analysisResults = info.value.analysisResults.filter(s => s.id !== sessionId)
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 模拟跳转到原始分析页
function viewOriginal(item) {
  if (item.fileId) {
     // 跳转到 trace 页，并带上 activeId 以高亮对应的 session
     router.push({
       path: `/trace/${item.fileId}`,
       query: { activeId: item.id }
     })
  } else {
     ElMessage.warning('无法跳转：文件ID缺失')
  }
}
</script>

<template>
  <div class="collection-detail-page" v-loading="loading">
    <!-- 顶部导航 -->
    <div class="nav-header">
      <el-page-header :icon="ArrowLeft" @back="router.back()">
        <template #content>
          <span class="header-title">合集详情</span>
        </template>
        <template #extra>
          <el-button type="primary" :icon="Reading" @click="startLearning" class="action-btn">
            AI 学习辅导
          </el-button>
        </template>
      </el-page-header>
    </div>

    <div class="content-wrapper">
      <!-- 头部概览卡片 -->
      <div class="hero-card compact">
        <div class="hero-content">
          <div class="hero-icon">
            <el-icon><FolderOpened /></el-icon>
          </div>
          <div class="hero-info">
             <div class="hero-header-row">
                <h1 class="title">{{ info.name }}</h1>
                <div class="meta-tags">
                  <el-tag effect="plain" round size="small" class="meta-tag">
                    {{ info.analysisResults?.length || 0 }} 个问题
                  </el-tag>
                  <span class="update-time" v-if="info.updatedAt">
                    更新于 {{ info.updatedAt }}
                  </span>
                </div>
             </div>
            <p class="description">{{ info.description || '暂无描述' }}</p>
          </div>
        </div>
      </div>

      <!-- 列表区域 -->
      <div class="list-section relative">
        <div class="section-header">
          <h3>收录问题列表</h3>
          <span class="subtitle">点击卡片查看详情</span>
        </div>
        
        <div v-if="info.analysisResults?.length" class="list-container">
          <AnimatedList 
            :items="info.analysisResults"
            @itemSelected="(item) => viewOriginal(item)"
          >
            <template #default="{ item, index, isSelected }">
              <div 
                class="problem-card"
                :class="{ 'is-active': isSelected }"
              >
                <div class="card-index">{{ String(index + 1).padStart(2, '0') }}</div>
                <div class="card-body">
                  <h4 class="problem-statement">{{ item.problemStatement }}</h4>
                </div>
                <div class="card-actions" @click.stop>
                  <el-button 
                    circle 
                    text 
                    type="danger" 
                    :icon="Delete" 
                    class="delete-btn"
                    @click="confirmRemove(item)"
                  />
                </div>
              </div>
            </template>
          </AnimatedList>
        </div>
        
        <div class="empty-state" v-else>
          <el-empty description="合集暂为空，去分析页添加一些问题吧" />
        </div>
      </div>
    </div>

    <!-- 智能学习抽屉 -->
    <el-drawer 
      v-model="quizVisible" 
      title="智能学习模式" 
      size="500px"
      direction="rtl"
      destroy-on-close
      class="quiz-drawer"
    >
      <div v-loading="quizLoading" class="quiz-container">
        <div v-if="quizQuestions.length > 0">
          <div class="quiz-progress-bar">
            <div class="progress-header">
              <span>进度 {{ currentQuestionIndex + 1 }}/{{ quizQuestions.length }}</span>
              <el-button 
                text 
                type="primary" 
                size="small" 
                :icon="RefreshRight"
                @click="handleResetQuiz"
              >
                重新答题
              </el-button>
            </div>
            <el-progress 
              :percentage="((currentQuestionIndex + 1) / quizQuestions.length) * 100" 
              :show-text="false"
              :stroke-width="8"
            />
          </div>
          
          <div class="question-card">
            <QuestionRenderer
              v-if="quizQuestions[currentQuestionIndex]"
              :question="quizQuestions[currentQuestionIndex].question"
              :type="quizQuestions[currentQuestionIndex].type || 'single_choice'"
              :options="quizQuestions[currentQuestionIndex].options || []"
              :correct-answer="quizQuestions[currentQuestionIndex].answer"
              :explanation="quizQuestions[currentQuestionIndex].explanation"
              :user-answer="currentUserAnswer"
              :is-submitted="showAnswer"
              :knowledge-point="quizQuestions[currentQuestionIndex].knowledgePoint"
              :key="quizQuestions[currentQuestionIndex].id"
              @answer-selected="handleQuestionAnswer"
            />
          </div>

          <div class="quiz-footer">
            <el-button 
              :disabled="currentQuestionIndex === 0"
              @click="prevQuestion"
              plain
            >
              上一题
            </el-button>
            <el-button 
              type="primary" 
              :disabled="currentQuestionIndex === quizQuestions.length - 1"
              @click="nextQuestion"
            >
              下一题
            </el-button>
          </div>
        </div>
        <el-empty v-else-if="!quizLoading" description="未能生成练习题，请稍后再试" />
      </div>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/variables';

// CSS Variables for local theme
.collection-detail-page {
  --page-padding: 32px;
  --card-radius: 16px;
  --transition-base: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--el-bg-color-page);
  overflow: hidden;
}

.nav-header {
  padding: 16px var(--page-padding);
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
  position: sticky;
  top: 0;
  z-index: 10;
  backdrop-filter: blur(10px);
  background-color: rgba(255, 255, 255, 0.8);
  
  .header-title {
    font-weight: 600;
    font-size: 16px;
    color: var(--el-text-color-primary);
  }
}

.dark .nav-header {
  background-color: rgba(0, 0, 0, 0.6);
}

.content-wrapper {
  padding: var(--page-padding);
  max-width: 1000px;
  margin: 0 auto;
  width: 100%;
  box-sizing: border-box;
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

// Hero Card Styles
.hero-card {
  background: var(--el-bg-color);
  border-radius: var(--card-radius);
  padding: 24px;
  margin-bottom: 24px;
  position: relative;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: var(--el-box-shadow-light);

  &.compact {
    .hero-content {
      align-items: center;
    }
    .hero-icon {
      width: 48px;
      height: 48px;
      font-size: 24px;
      border-radius: 12px;
    }
    .title {
      font-size: 20px;
      margin: 0;
    }
    .description {
      margin: 8px 0 0 0;
      font-size: 14px;
      line-height: 1.5;
    }
  }

  .hero-content {
    position: relative;
    z-index: 2;
    display: flex;
    gap: 16px;
  }

  .hero-icon {
    width: 64px;
    height: 64px;
    background: var(--el-color-primary-light-9);
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    color: var(--el-color-primary);
    flex-shrink: 0;
  }

  .hero-info {
    flex: 1;

    .hero-header-row {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .title {
      font-size: 24px;
      margin: 0 0 8px 0;
      color: var(--el-text-color-primary);
      font-weight: 700;
    }

    .description {
      font-size: 14px;
      color: var(--el-text-color-regular);
      margin: 0 0 16px 0;
      line-height: 1.6;
      max-width: 800px;
    }

    .meta-tags {
      display: flex;
      align-items: center;
      gap: 12px;

      .update-time {
        display: flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
    }
  }
}

// List Section Styles
.list-section {
    position: relative;
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
    
    .section-header {
    display: flex;
    align-items: baseline;
    gap: 12px;
    margin-bottom: 24px;
    flex-shrink: 0;

    h3 {
      font-size: 20px;
      margin: 0;
      font-weight: 600;
      color: var(--el-text-color-primary);
    }

    .subtitle {
      font-size: 13px;
      color: var(--el-text-color-secondary);
    }
  }

  .list-container {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    overflow-x: hidden;
  }
}

.problem-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: var(--transition-base);
  position: relative;

  &.is-active, &:hover {
    background-color: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary-light-5);

    .card-actions .delete-btn {
      opacity: 1;
    }
  }

  .card-index {
    font-size: 14px;
    font-family: monospace;
    font-weight: 700;
    color: var(--el-text-color-placeholder);
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--el-fill-color-light);
    border-radius: 8px;
    transition: var(--transition-base);
    flex-shrink: 0;
  }

  .card-body {
    flex: 1;
    min-width: 0; // Fix truncate inside flex
    padding-top: 0;

    .problem-statement {
      font-size: 15px;
      font-weight: 500;
      color: var(--el-text-color-primary);
      margin: 0;
      line-height: 1.5;
    }
  }

  .card-actions {
    align-self: center;
    
    .delete-btn {
      opacity: 0;
      transition: opacity 0.2s;
      color: var(--el-color-danger); /* Ensure icon color is visible */
      
      &:hover {
        background-color: var(--el-color-danger-light-9);
      }

      /* Force icon size and visibility */
      :deep(.el-icon) {
        font-size: 16px;
        vertical-align: middle;
      }
    }
  }
}

// Quiz Drawer Styles
.quiz-container {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.quiz-progress-bar {
  margin-bottom: 24px;

  .progress-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}

.question-card {
  flex: 1;
  overflow-y: auto;
}

.quiz-footer {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--el-border-color-light);
  display: flex;
  justify-content: space-between;
}

// Transitions
.list-enter-active,
.list-leave-active {
  transition: all 0.4s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

// Responsive
@media (max-width: 768px) {
  .collection-detail-page {
    --page-padding: 16px;
  }
  
  .hero-card {
    padding: 24px;
    flex-direction: column;
    
    .hero-content {
      flex-direction: column;
      align-items: flex-start;
    }
    
    .hero-icon {
      width: 60px;
      height: 60px;
      font-size: 30px;
    }
    
    .title {
      font-size: 24px !important;
    }
  }

  .problem-card {
    padding: 16px;
    gap: 12px;
    
    .card-index {
      width: 32px;
      height: 32px;
      font-size: 14px;
    }
    
    .delete-btn {
      opacity: 1; // Always show on mobile
    }
  }
}
</style>
