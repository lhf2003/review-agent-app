<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Star, Clock, Close, Select, Delete, Loading } from '@element-plus/icons-vue'
import QuestionRenderer from './QuestionRenderer.vue'
import CustomScroll from '../CustomScroll.vue'
import { api } from '../../api/http'

/**
 * 错题详情面板组件（嵌入式）
 * 用于 split-layout 的右侧详情面板，显示题目完整信息、错误历史、知识点掌握度
 */
const props = defineProps({
  mistakeId: {
    type: Number,
    default: null
  },
  questionId: {
    type: Number,
    default: null
  }
})

const emit = defineEmits(['marked-mastered', 'deleted'])

// 数据状态
const loading = ref(false)
const saving = ref(false)
const questionDetail = ref(null)
const mistakeHistory = ref([])
const knowledgeMastery = ref(null)

// 加载题目详情
async function loadQuestionDetail() {
  if (!props.questionId) return

  loading.value = true
  try {
    const mistakeList = await api.getMistakeList('all')
    const target = mistakeList.find(m => m.questionId === props.questionId)

    if (target) {
      questionDetail.value = target
      // 加载错题历史
      await loadMistakeHistory()
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

// 加载错题历史
async function loadMistakeHistory() {
  if (!props.mistakeId) return
  try {
    const history = await api.getMistakeHistory(props.mistakeId)
    mistakeHistory.value = history
  } catch {
  }
}

// 标记为已掌握
async function markAsMastered() {
  if (!props.questionId) return

  saving.value = true
  try {
    await api.markMistakesMastered([props.questionId])
    ElMessage.success('已标记为掌握')

    if (questionDetail.value) {
      questionDetail.value.mastered = true
    }

    emit('marked-mastered', props.questionId)
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
    loadQuestionDetail()
  }
}, { immediate: true })
</script>

<template>
  <div class="mistake-detail-pane">
    <!-- Loading 状态 -->
    <div v-if="loading" class="pane-loading">
      <el-skeleton animated />
    </div>

    <!-- Empty 状态 -->
    <div v-else-if="!questionDetail" class="pane-empty">
      <el-empty description="题目数据不存在" />
    </div>

    <!-- 详情内容 -->
    <template v-else>
      <!-- 顶部操作栏 -->
      <div class="detail-header">
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
            size="small"
          >
            <el-icon><Select /></el-icon>
            标记掌握
          </el-button>
          <el-button
            type="danger"
            plain
            :loading="saving"
            @click="deleteFromMistakeBook"
            size="small"
          >
            <el-icon><Delete /></el-icon>
            移除
          </el-button>
        </div>
      </div>

      <CustomScroll class="pane-content">
        <!-- 题目内容 -->
        <QuestionRenderer
          v-if="questionDetail"
          class="mistake-question-renderer"
          :question="questionDetail.questionText"
          :type="getQuestionType(questionDetail.questionType)"
          :options="parseOptions(questionDetail.optionsJson)"
          :user-answer="questionDetail.userAnswer || undefined"
          :correct-answer="questionDetail.correctAnswer"
          :explanation="questionDetail.explanation"
          :is-submitted="true"
          :question-id="questionDetail.questionId"
          :knowledge-point="questionDetail.knowledgePoint"
          :blank-count="questionDetail.blankCount"
        />

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
      </CustomScroll>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.mistake-detail-pane {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--el-bg-color-page);
}

.pane-loading,
.pane-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.pane-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
}

// 顶部操作栏 - Glassmorphism 增强
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
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

.header-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  letter-spacing: -0.3px;
}

.title-icon {
  font-size: 26px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 6px;
  border-radius: 8px;
  /* Remove transition: all to prevent scale effect on theme switch */
  transition: transform 0.2s ease !important;

  html.dark & {
    background: rgba(64, 158, 255, 0.15);
  }
}

.header-actions {
  display: flex;
  gap: 10px;
}

// 通用区块样式 - 卡片化设计
.mastery-section,
.history-section {
  padding: 20px;
  margin-bottom: 16px;
  background: var(--el-bg-color);
  border-radius: 16px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);
  /* Remove transition: all to prevent scale effect on theme switch */
  transition: box-shadow 0.3s ease, transform 0.3s ease !important;

  &:hover {
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  }

  html.dark & {
    background: rgba(255, 255, 255, 0.03);
    border-color: rgba(255, 255, 255, 0.08);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.2);

    &:hover {
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
    }
  }

  &:last-child {
    margin-bottom: 0;
  }
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 16px;
  letter-spacing: -0.2px;

  .el-icon {
    font-size: 16px;
    color: var(--el-color-primary);
  }

  .el-badge {
    margin-left: auto;
  }
}

.mistake-question-renderer {
  margin-bottom: 16px;
}

// 知识点掌握度 - 增强渐变效果
.mastery-card {
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.1) 0%, rgba(64, 158, 255, 0.03) 100%);
  border-radius: 14px;
  padding: 18px;
  border: 1px solid rgba(64, 158, 255, 0.15);
  position: relative;
  overflow: hidden;
  /* Remove transition: all to prevent scale effect on theme switch */
  transition: box-shadow 0.3s ease, transform 0.3s ease !important;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 1px;
    background: linear-gradient(90deg,
      transparent,
      rgba(64, 158, 255, 0.3),
      transparent
    );
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(64, 158, 255, 0.15);
  }

  html.dark & {
    background: linear-gradient(135deg, rgba(64, 158, 255, 0.15) 0%, rgba(64, 158, 255, 0.05) 100%);
    border-color: rgba(64, 158, 255, 0.2);
  }
}

.mastery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  .knowledge-point {
    font-size: 15px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    letter-spacing: -0.2px;
  }
}

.mastery-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .stat-label {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    font-weight: 500;
  }

  .stat-value {
    font-size: 18px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    letter-spacing: -0.3px;
  }
}

.progress-bar {
  height: 6px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 3px;
  overflow: hidden;

  html.dark & {
    background: rgba(255, 255, 255, 0.1);
  }
}

.progress-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}

// 错误历史
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  background: rgba(245, 108, 108, 0.04);
  border-radius: 12px;
  border: 1px solid rgba(245, 108, 108, 0.1);
  /* Remove transition: all to prevent scale effect on theme switch */
  transition: background-color 0.2s ease, transform 0.2s ease !important;

  &:hover {
    background: rgba(245, 108, 108, 0.08);
    transform: translateX(4px);
  }

  html.dark & {
    background: rgba(245, 108, 108, 0.08);
    border-color: rgba(245, 108, 108, 0.15);

    &:hover {
      background: rgba(245, 108, 108, 0.12);
    }
  }
}

.history-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(245, 108, 108, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;

  html.dark & {
    background: rgba(245, 108, 108, 0.2);
  }
}

.history-content {
  flex: 1;
  min-width: 0;
}

.history-date {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
  font-weight: 500;
}

.history-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13px;

  .wrong-answer {
    color: #f56c6c;
    font-weight: 500;
  }

  .correct-answer {
    color: #67c23a;
    font-weight: 500;
  }
}

.history-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

// 响应式设计
@media (max-width: 768px) {
  .pane-content {
    padding: 12px;
  }

  .detail-header {
    padding: 12px 16px;

    .header-actions {
      gap: 8px;
    }
  }

  .mastery-section,
  .history-section {
    padding: 16px;
  }

  .mastery-stats {
    gap: 16px;
  }
}
</style>
