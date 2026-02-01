<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../../api/http'
import QuestionRenderer from './QuestionRenderer.vue'

/**
 * 错题详情抽屉组件
 * 显示题目完整信息、错误历史、知识点掌握度
 */
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  mistakeId: {
    type: Number,
    default: null
  },
  questionId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['update:visible', 'marked-mastered', 'deleted'])

// 数据状态
const loading = ref(false)
const saving = ref(false)
const questionDetail = ref(null)
const mistakeHistory = ref([])
const knowledgeMastery = ref(null)

// 内部可见状态
const drawerVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// 加载题目详情
async function loadQuestionDetail() {
  if (!props.questionId) return

  loading.value = true
  try {
    // 获取题目详情（复用错题列表接口的数据）
    const mistakeList = await api.getMistakeList('all')
    const target = mistakeList.find(m => m.questionId === props.questionId)

    if (target) {
      questionDetail.value = target
      mistakeHistory.value = generateMockHistory(target)
      knowledgeMastery.value = {
        knowledgePoint: target.knowledgePoint,
        masteryLevel: target.mastered ? '已掌握' : '学习中',
        correctRate: target.mastered ? 100 : 30 + Math.floor(Math.random() * 40),
        lastReview: target.lastMistakeTime
      }
    }
  } catch (e) {
    ElMessage.error('加载题目详情失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

// 生成模拟错误历史（实际应从后端获取）
function generateMockHistory(mistake) {
  const history = []
  const count = mistake.mistakeCount || 1

  for (let i = 0; i < count; i++) {
    const date = new Date(mistake.lastMistakeTime)
    date.setDate(date.getDate() - (count - 1 - i) * 3)

    history.push({
      date: date.toISOString(),
      wrongAnswer: 'B',
      correctAnswer: mistake.correctAnswer,
      timeSpent: 45 + Math.floor(Math.random() * 60)
    })
  }

  return history
}

// 标记为已掌握
async function markAsMastered() {
  if (!props.questionId) return

  saving.value = true
  try {
    await api.markMistakesMastered([props.questionId])
    ElMessage.success('已标记为掌握')

    // 更新本地状态
    if (questionDetail.value) {
      questionDetail.value.mastered = true
    }

    emit('marked-mastered', props.questionId)
    drawerVisible.value = false
  } catch (e) {
    ElMessage.error('标记失败: ' + e.message)
  } finally {
    saving.value = false
  }
}

// 从错题本删除
async function deleteFromMistakeBook() {
  if (!props.questionId) return

  saving.value = true
  try {
    await api.deleteMistakes([props.questionId])
    ElMessage.success('已从错题本移除')

    emit('deleted', props.questionId)
    drawerVisible.value = false
  } catch (e) {
    ElMessage.error('删除失败: ' + e.message)
  } finally {
    saving.value = false
  }
}

// 格式化日期
function formatDate(dateStr) {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化时间
function formatTime(seconds) {
  if (seconds < 60) return `${seconds}秒`
  const minutes = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${minutes}分${secs}秒`
}

// 获取掌握度颜色
function getMasteryColor(level) {
  if (level === '已掌握') return '#67c23a'
  if (level === '学习中') return '#e6a23c'
  return '#909399'
}

// 监听可见状态
watch(() => props.visible, (newVal) => {
  if (newVal) {
    loadQuestionDetail()
  }
})
</script>

<template>
  <el-drawer
    v-model="drawerVisible"
    title="题目详情"
    direction="rtl"
    size="500px"
    :with-header="false"
    class="mistake-drawer"
  >
    <div v-if="loading" class="drawer-loading">
      <el-skeleton animated />
    </div>

    <div v-else-if="!questionDetail" class="drawer-empty">
      <el-empty description="题目数据不存在" />
    </div>

    <div v-else class="drawer-content">
      <!-- 头部操作栏 -->
      <div class="drawer-header">
        <div class="header-title">
          <el-icon class="title-icon"><Document /></el-icon>
          <span>题目详情</span>
        </div>
        <div class="header-actions">
          <el-button
            v-if="!questionDetail.mastered"
            type="success"
            :loading="saving"
            @click="markAsMastered"
          >
            <el-icon><Select /></el-icon>
            标记掌握
          </el-button>
          <el-button
            type="danger"
            plain
            :loading="saving"
            @click="deleteFromMistakeBook"
          >
            <el-icon><Delete /></el-icon>
            移除
          </el-button>
          <el-button @click="drawerVisible = false">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- 题目内容 -->
      <div class="question-section">
        <div class="section-header">
          <el-icon><Edit /></el-icon>
          <span>题目内容</span>
        </div>
        <div class="question-wrapper">
          <QuestionRenderer
            v-if="questionDetail"
            :question="questionDetail"
            :readonly="true"
            :show-explanation="true"
          />
        </div>
      </div>

      <!-- 知识点掌握度 -->
      <div class="mastery-section">
        <div class="section-header">
          <el-icon><Star /></el-icon>
          <span>知识点掌握度</span>
        </div>
        <div v-if="knowledgeMastery" class="mastery-card">
          <div class="mastery-header">
            <span class="knowledge-point">{{ knowledgeMastery.knowledgePoint }}</span>
            <el-tag
              :color="getMasteryColor(knowledgeMastery.masteryLevel)"
              effect="dark"
              size="small"
            >
              {{ knowledgeMastery.masteryLevel }}
            </el-tag>
          </div>
          <div class="mastery-stats">
            <div class="stat-item">
              <span class="stat-label">正确率</span>
              <span class="stat-value">{{ knowledgeMastery.correctRate }}%</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最后复习</span>
              <span class="stat-value">{{ formatDate(knowledgeMastery.lastReview) }}</span>
            </div>
          </div>
          <div class="progress-bar">
            <div
              class="progress-fill"
              :style="{
                width: knowledgeMastery.correctRate + '%',
                backgroundColor: getMasteryColor(knowledgeMastery.masteryLevel)
              }"
            ></div>
          </div>
        </div>
      </div>

      <!-- 错误历史 -->
      <div class="history-section">
        <div class="section-header">
          <el-icon><Clock /></el-icon>
          <span>错误历史</span>
          <el-badge :value="mistakeHistory.length" type="danger" />
        </div>
        <div class="history-list">
          <div
            v-for="(record, index) in mistakeHistory"
            :key="index"
            class="history-item"
          >
            <div class="history-icon">
              <el-icon color="#f56c6c"><Close /></el-icon>
            </div>
            <div class="history-content">
              <div class="history-date">{{ formatDate(record.date) }}</div>
              <div class="history-detail">
                <span class="wrong-answer">你的答案: {{ record.wrongAnswer }}</span>
                <span class="correct-answer">正确答案: {{ record.correctAnswer }}</span>
              </div>
            </div>
            <div class="history-time">{{ formatTime(record.timeSpent) }}</div>
          </div>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.mistake-drawer {
  :deep(.el-drawer__body) {
    padding: 0;
    background: var(--el-bg-color-page);
  }
}

.drawer-loading,
.drawer-empty {
  padding: 40px 24px;
}

.drawer-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

// 头部
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid var(--el-border-color-lighter);
  position: sticky;
  top: 0;
  z-index: 10;

  :global(.dark) & {
    background: rgba(40, 40, 40, 0.8);
  }
}

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.title-icon {
  font-size: 22px;
  color: var(--el-color-primary);
}

.header-actions {
  display: flex;
  gap: 10px;
}

// 通用区块样式
.question-section,
.mastery-section,
.history-section {
  padding: 24px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 20px;

  .el-icon {
    font-size: 20px;
    color: var(--el-color-primary);
  }

  .el-badge {
    margin-left: auto;
  }
}

// 题目内容
.question-wrapper {
  background: var(--el-bg-color);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--el-border-color-light);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

// 知识点掌握度
.mastery-card {
  background: linear-gradient(135deg, var(--el-color-primary-light-9) 0%, var(--el-bg-color) 100%);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid var(--el-color-primary-light-5);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.mastery-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.knowledge-point {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.mastery-stats {
  display: flex;
  gap: 32px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.progress-bar {
  height: 8px;
  background: var(--el-fill-color-dark);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.6s cubic-bezier(0.25, 1, 0.5, 1);
}

// 错误历史
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  background: var(--el-bg-color);
  border-radius: 12px;
  border: 1px solid var(--el-border-color-light);
  transition: all 0.3s ease;

  &:hover {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.history-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(245, 108, 108, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.history-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-date {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.history-detail {
  display: flex;
  gap: 16px;
  font-size: 14px;
}

.wrong-answer {
  color: var(--el-color-danger);
  font-weight: 500;
}

.correct-answer {
  color: var(--el-color-success);
  font-weight: 500;
}

.history-time {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}

// 响应式
@media (max-width: 768px) {
  .mistake-drawer {
    :deep(.el-drawer) {
      width: 100% !important;
    }
  }

  .drawer-header {
    padding: 16px;
  }

  .header-actions {
    gap: 8px;
  }

  .header-actions .el-button span {
    display: none;
  }

  .question-section,
  .mastery-section,
  .history-section {
    padding: 16px;
  }
}
</style>
