<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { TrendCharts, CircleCheck, Clock } from '@element-plus/icons-vue'
import { api } from '../../api/http'
import CustomScroll from '../../components/CustomScroll.vue'

const router = useRouter()

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false
  },
  selectedQuizId: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['select-quiz'])

// 筛选条件
const statusFilter = ref('all')
const collectionFilter = ref(null)
const collectionOptions = ref([])

// 习题历史数据
const quizHistory = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const totalItems = ref(0)
const loading = ref(false)

/**
 * 加载习题历史
 */
async function loadHistory() {
  loading.value = true
  try {
    // 将字符串值映射为 API 需要的数字值
    const statusMap = {
      'all': null,
      'completed': 1,
      'progress': 0
    }
    const response = await api.getQuizHistory({
      status: statusMap[statusFilter.value],
      collectionId: collectionFilter.value,
      page: currentPage.value - 1,
      size: pageSize.value
    })
    quizHistory.value = response.content || []
    totalItems.value = response.totalElements || 0
  } catch (error) {
    ElMessage.error('加载习题历史失败: ' + error.message)
  } finally {
    loading.value = false
  }
}

/**
 * 跳转到习题详情
 */
function goToDetail(quizId) {
  // Ensure quizId is converted to String
  const quizIdStr = String(quizId)
  if (props.embedded) {
    emit('select-quiz', quizIdStr)
  } else {
    router.push(`/quiz-history/${quizIdStr}`)
  }
}

/**
 * 获取状态样式
 */
function getStatusClass(quiz) {
  if (quiz.isOutdated) return 'status-outdated'
  if (quiz.status === 1) return 'status-completed'
  return 'status-progress'
}

/**
 * 获取状态文本
 */
function getStatusText(quiz) {
  if (quiz.isOutdated) return '已过期'
  if (quiz.status === 1) return '已完成'
  return '进行中'
}

/**
 * 获取分数颜色
 */
function getScoreClass(score) {
  if (score >= 90) return 'score-excellent'
  if (score >= 75) return 'score-good'
  if (score >= 60) return 'score-pass'
  return 'score-fail'
}

/**
 * 加载合集列表
 */
async function loadCollections() {
  try {
    const result = await api.getCollectionList()
    collectionOptions.value = result.list || []
  } catch (error) {
    
  }
}

/**
 * 页码变化
 */
function handlePageChange(page) {
  currentPage.value = page
  loadHistory()
}

onMounted(() => {
  loadHistory()
  loadCollections()
})
</script>

<template>
  <div class="quiz-history-page">
    <!-- 筛选器栏 -->
    <div class="filter-bar glass-card">
      <div class="filter-controls">
        <el-radio-group v-model="statusFilter" @change="loadHistory" class="filter-tabs">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="completed">已完成</el-radio-button>
          <el-radio-button value="progress">进行中</el-radio-button>
        </el-radio-group>

        <el-select
          v-model="collectionFilter"
          @change="loadHistory"
          placeholder="选择合集"
          clearable
          class="collection-select"
          size="small"
        >
          <el-option
            v-for="collection in collectionOptions"
            :key="collection.id"
            :label="collection.name"
            :value="collection.id"
          />
        </el-select>
      </div>
    </div>

    <!-- 加载状态 (骨架屏) -->
    <div v-if="loading && quizHistory.length === 0" class="quiz-list-wrapper">
      <CustomScroll>
        <div class="quiz-list">
          <div v-for="i in 6" :key="i" class="quiz-card glass-card">
            <el-skeleton animated>
              <template #template>
                <div class="card-header" style="margin-bottom: 12px;">
                  <el-skeleton-item variant="h3" style="width: 50%" />
                  <el-skeleton-item variant="text" style="width: 40px" />
                </div>
                <div class="card-stats">
                  <div class="stat-group">
                    <el-skeleton-item variant="text" style="width: 60px" />
                    <el-skeleton-item variant="text" style="width: 60px" />
                  </div>
                  <el-skeleton-item variant="text" style="width: 100px" />
                </div>
              </template>
            </el-skeleton>
          </div>
        </div>
      </CustomScroll>
    </div>

    <!-- 习题列表 -->
    <div v-else-if="quizHistory.length > 0" class="quiz-list-wrapper">
      <CustomScroll>
        <div class="quiz-list">
          <div
            v-for="quiz in quizHistory"
            :key="quiz.quizId"
            :class="['quiz-card', 'glass-card', { 'is-selected': String(quiz.quizId) === props.selectedQuizId }]"
            @click="goToDetail(quiz.quizId)"
          >
            <div class="card-header">
              <h3 class="card-title">{{ quiz.collectionName }}</h3>
              <span :class="['status-badge', getStatusClass(quiz)]">
                {{ getStatusText(quiz) }}
              </span>
            </div>

            <div class="card-stats">
              <div class="stat-group">
                <div class="stat-item">
                  <el-icon class="stat-icon"><TrendCharts /></el-icon>
                  <span class="stat-label">得分</span>
                  <span :class="['stat-value', getScoreClass(quiz.totalScore)]">
                    {{ quiz.totalScore ?? '-' }}
                  </span>
                </div>
                <div class="stat-item">
                  <el-icon class="stat-icon"><CircleCheck /></el-icon>
                  <span class="stat-label">正确</span>
                  <span class="stat-value">{{ quiz.correctCount }}/{{ quiz.questionCount }}</span>
                </div>
              </div>

              <div class="stat-item stat-time">
                <el-icon class="stat-icon"><Clock /></el-icon>
                <span class="stat-value">{{ quiz.createdTime }}</span>
              </div>
            </div>
          </div>
        </div>
      </CustomScroll>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state glass-card">
      <div class="empty-icon">
        <el-icon :size="64"><TrendCharts /></el-icon>
      </div>
      <h3 class="empty-title">暂无习题记录</h3>
      <p class="empty-desc">创建题库后，即可开始练习巩固知识点</p>
      <button class="create-btn" @click="router.push('/collections')">
        去创建题库
      </button>
    </div>

    <!-- 分页 -->
    <div v-if="totalItems > 0" class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="totalItems"
        layout="prev, pager, next, total"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.quiz-history-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
}

/* ============ Glassmorphism 效果 ============ */
.glass-card {
  background: rgba(255, 255, 255, 0.64);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.24);
  box-shadow: 0 2px 16px -1px rgba(0, 0, 0, 0.04);
  /* Remove transition: all to prevent scale effect on theme switch */
  transition: box-shadow 0.25s ease, transform 0.25s ease !important;
}

.glass-card:hover {
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 8px 32px -2px rgba(0, 0, 0, 0.08);
}

/* ============ 筛选栏 ============ */
.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  height: 52px;
  min-height: 52px;
  margin: -16px -16px 16px -16px;
  border-radius: 0;
  border-left: none;
  border-right: none;
  border-top: none;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  gap: 12px;
  box-sizing: border-box;
}

.filter-bar:hover {
  background: rgba(255, 255, 255, 0.9);
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
  overflow-x: auto;
}

.filter-tabs {
  --el-fill-color-light: transparent;
  display: flex;
  gap: 3px;
  background: rgba(0, 0, 0, 0.04);
  padding: 3px;
  border-radius: 10px;
  flex-shrink: 0;
}

.filter-tabs :deep(.el-radio-button__inner) {
  border: none !important;
  background: transparent !important;
  border-radius: 8px !important;
  padding: 6px 12px !important;
  margin-right: 0 !important;
  color: #86868b !important;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  /* Remove transition: all to prevent scale effect on theme switch */
  transition: background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease !important;
  box-shadow: none !important;
}

.filter-tabs :deep(.el-radio-button__inner:hover) {
  color: #1d1d1f !important;
  background: transparent !important;
}

.filter-tabs :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner),
.filter-tabs :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.9) !important;
  color: #1d1d1f !important;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08) !important;
  border-color: transparent !important;
}

/* 确保首尾按钮圆角正确 */
.filter-tabs :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: 8px !important;
}

.filter-tabs :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 8px !important;
}

/* 深色模式适配 */
html.dark .filter-tabs {
  background: rgba(255, 255, 255, 0.08);
}

html.dark .filter-tabs :deep(.el-radio-button__inner) {
  color: #86868b !important;
  background: transparent !important;
  border-color: transparent !important;
  box-shadow: none !important;
}

html.dark .filter-tabs :deep(.el-radio-button__inner:hover) {
  color: #f5f5f7 !important;
  background: transparent !important;
}

html.dark .filter-tabs :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner),
html.dark .filter-tabs :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.15) !important;
  color: #f5f5f7 !important;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08) !important;
  border-color: transparent !important;
}

/* 深色模式首尾按钮圆角 */
html.dark .filter-tabs :deep(.el-radio-button:first-child .el-radio-button__inner),
html.dark .filter-tabs :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 8px !important;
}

.collection-select {
  width: 140px;
}

.collection-select :deep(.el-input__wrapper) {
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: none;
  /* Remove transition: all to prevent scale effect on theme switch */
  transition: border-color 0.2s ease, background-color 0.2s ease !important;
  padding: 1px 11px;
}

.collection-select :deep(.el-input__wrapper:hover) {
  border-color: rgba(0, 0, 0, 0.12);
}

.collection-select :deep(.el-input__wrapper.is-focus) {
    border-color: #0071e3;
    background: white;
  }

/* ============ 习题列表 ============ */
.quiz-list-wrapper {
  flex: 1;
  min-height: 0; /* Important for flex child scroll */
}

.quiz-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px;
}

.quiz-card {
  padding: 16px;
  border-radius: 16px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quiz-card:hover {
  transform: scale(1.01);
  background: rgba(255, 255, 255, 0.9);
}

.quiz-card:active {
  transform: scale(0.99);
}

.quiz-card.is-selected {
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(var(--el-color-primary-rgb), 0.4);
  box-shadow:
    0 0 0 1px rgba(var(--el-color-primary-rgb), 0.15) inset,
    0 4px 16px -2px rgba(var(--el-color-primary-rgb), 0.12),
    0 8px 24px -4px rgba(0, 0, 0, 0.08);
}

.quiz-card.is-selected:hover {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(var(--el-color-primary-rgb), 0.5);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 0;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
  color: #1d1d1f;
  letter-spacing: -0.01em;
  line-height: 1.4;
  margin-right: 8px;
}

/* ============ 状态标签 ============ */
.status-badge {
  padding: 4px 8px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 6px;
  white-space: nowrap;
  flex-shrink: 0;
}

.status-completed {
  background: rgba(52, 199, 89, 0.12);
  color: #34c759;
}

.status-progress {
  background: rgba(0, 122, 255, 0.12);
  color: #007aff;
}

.status-outdated {
  background: rgba(255, 149, 0, 0.12);
  color: #ff9500;
}

/* ============ 统计信息 ============ */
.card-stats {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.stat-group {
  display: flex;
  gap: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-icon {
  font-size: 14px;
  color: #86868b;
}

.stat-label {
  font-size: 12px;
  color: #86868b;
  display: none; /* Hide label to save space in compact view */
}

.stat-value {
  font-size: 13px;
  font-weight: 600;
  color: #1d1d1f;
}

.stat-time {
  color: #86868b;
  font-size: 12px;
}

.stat-time .stat-value {
  font-weight: 400;
  color: #86868b;
}

/* ============ 分数颜色 ============ */
.score-excellent { color: #34c759; }
.score-good { color: #007aff; }
.score-pass { color: #ff9500; }
.score-fail { color: #ff3b30; }

/* ============ 空状态 ============ */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  border-radius: 16px;
  margin-top: 40px;
}

.empty-icon {
  color: #c7c7cc;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 6px 0;
}

.empty-desc {
  font-size: 13px;
  color: #86868b;
  margin: 0 0 20px 0;
}

.create-btn {
  padding: 8px 16px;
  background: #0071e3;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  /* Remove transition: all to prevent scale effect on theme switch */
  transition: background-color 0.2s ease, transform 0.2s ease !important;
}

.create-btn:hover {
  background: #0077ed;
  transform: scale(1.02);
}

.create-btn:active {
  transform: scale(0.98);
}

/* ============ 分页 ============ */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 16px;
  padding: 0;
}

.pagination-container :deep(.el-pagination) {
  gap: 4px;
  --el-pagination-button-width: 28px;
  --el-pagination-button-height: 28px;
}

.pagination-container :deep(.el-pager li),
.pagination-container :deep(.btn-prev),
.pagination-container :deep(.btn-next) {
  min-width: 28px;
  height: 28px;
  border-radius: 6px;
  font-weight: 500;
  /* Remove transition: all to prevent scale effect on theme switch */
  transition: background-color 0.2s ease, color 0.2s ease !important;
  font-size: 12px;
}

.pagination-container :deep(.el-pager li.is-active) {
  background: #0071e3;
  border-color: #0071e3;
}

/* ============ 响应式 ============ */
@media (max-width: 1200px) {
  /* No special handling needed as it's flex column */
}

@media (max-width: 768px) {
  .quiz-history-page {
    padding: 16px;
  }
}

@media (max-width: 640px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  
  .filter-controls {
    flex-wrap: wrap;
  }

  .collection-select {
    width: 100%;
    flex: 1;
  }
}

/* ============ html.dark 深色模式兼容 ============ */
html.dark .quiz-history-page {
  .glass-header {
    background: rgba(30, 30, 30, 0.72);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .glass-card {
    background: rgba(40, 40, 42, 0.64);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .glass-card:hover {
    background: rgba(50, 50, 52, 0.8);
  }

  .quiz-card.is-selected {
    background: rgba(50, 50, 52, 0.8);
    border-color: rgba(var(--el-color-primary-rgb), 0.35);
    box-shadow:
      0 0 0 1px rgba(var(--el-color-primary-rgb), 0.12) inset,
      0 4px 16px -2px rgba(var(--el-color-primary-rgb), 0.15),
      0 8px 24px -4px rgba(0, 0, 0, 0.2);
  }

  .quiz-card.is-selected:hover {
    background: rgba(55, 55, 57, 0.9);
    border-color: rgba(var(--el-color-primary-rgb), 0.45);
  }

  .filter-bar {
    background: rgba(40, 40, 42, 0.64);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    border-left: none;
    border-right: none;
    border-top: none;
  }

  .card-title {
    color: #f5f5f7;
  }

  .stat-icon,
  .stat-label,
  .stat-time {
    color: #86868b;
  }

  .stat-time .stat-value {
    color: #86868b;
  }

  .stat-value {
    color: #f5f5f7;
  }

  .empty-icon {
    color: #48484a;
  }

  .empty-title {
    color: #f5f5f7;
  }

  .empty-desc {
    color: #86868b;
  }
}
</style>
