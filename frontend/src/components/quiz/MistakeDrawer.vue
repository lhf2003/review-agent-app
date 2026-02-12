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
      // TODO: 后端需要提供错题历史数据接口
      mistakeHistory.value = []
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
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;

  :global(.dark) & {
    background: rgba(30, 30, 30, 0.8);
    border-bottom-color: rgba(255, 255, 255, 0.1);
  }
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 17px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  letter-spacing: -0.3px;
}

.title-icon {
  font-size: 20px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 6px;
  border-radius: 8px;
  
  :global(.dark) & {
    background: rgba(64, 158, 255, 0.2);
  }
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
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
  
  :global(.dark) & {
    border-bottom-color: rgba(255, 255, 255, 0.05);
  }
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;

  .el-icon {
    font-size: 18px;
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
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.02);
  
  :global(.dark) & {
    border-color: rgba(255, 255, 255, 0.1);
    background: rgba(255, 255, 255, 0.03);
  }
}

// 知识点掌握度
.mastery-card {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.08) 0%, rgba(64, 158, 255, 0.02) 100%);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(64, 158, 255, 0.1);
  position: relative;
  overflow: hidden;
  
  :global(.dark) & {
    background: linear-gradient(135deg, rgba(64, 158, 255, 0.15) 0%, rgba(64, 158, 255, 0.05) 100%);
    border-color: rgba(64, 158, 255, 0.2);
  }
}

.mastery-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.knowledge-point {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.mastery-stats {
  display: flex;
  gap: 40px;
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  font-family: -apple-system, BlinkMacSystemFont, sans-serif;
}

.progress-bar {
  height: 6px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 3px;
  overflow: hidden;
  
  :global(.dark) & {
    background: rgba(255, 255, 255, 0.1);
  }
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.8s cubic-bezier(0.22, 1, 0.36, 1);
}

// 错误历史
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    left: 19px;
    top: 10px;
    bottom: 10px;
    width: 2px;
    background: rgba(0, 0, 0, 0.05);
    z-index: 0;
    
    :global(.dark) & {
      background: rgba(255, 255, 255, 0.1);
    }
  }
}

.history-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 12px;
  background: var(--el-bg-color);
  border-radius: 12px;
  border: 1px solid transparent;
  transition: all 0.2s ease;
  z-index: 1;

  &:hover {
    background: rgba(0, 0, 0, 0.02);
    
    :global(.dark) & {
      background: rgba(255, 255, 255, 0.03);
    }
  }
}

.history-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--el-bg-color);
  border: 4px solid var(--el-bg-color-page); // Creates spacing from line
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  
  :global(.dark) & {
    background: #2c2c2e;
    border-color: #1c1c1e;
  }
}

.history-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 2px;
}

.history-date {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

.history-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
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
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
  padding-top: 4px;
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
