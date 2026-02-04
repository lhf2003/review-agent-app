<script setup>
import { ref, computed, onMounted } from 'vue'
import { Search, RefreshRight, FolderChecked, Delete, Loading, PriceTag, Clock, SuccessFilled, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import CustomScroll from '../CustomScroll.vue'
import { api } from '../../api/http'

/**
 * 错题列表面板组件（精简版）
 * 用于 split-layout 的左侧列表，去除标题栏和统计卡片
 */
const props = defineProps({
  embedded: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['select-mistake'])

const loading = ref(false)
const searchKeyword = ref('')

// 数据
const mistakes = ref([])
const selectedMistakes = ref(new Set())

// 筛选方式：'all' | 'unmastered' | 'mastered'
const filterMode = ref('all')

// 搜索状态
const isSearching = ref(false)

onMounted(() => {
  fetchMistakes()
})

// 获取错题列表
async function fetchMistakes() {
  loading.value = true
  try {
    const data = await api.getMistakeList(filterMode.value)
    // 转换数据格式以适配前端组件
    mistakes.value = data.map(m => ({
      id: m.id,
      questionId: m.questionId,
      question: m.questionText,
      questionType: m.questionType,
      options: m.optionsJson ? JSON.parse(m.optionsJson) : [],
      answer: m.correctAnswer,
      explanation: m.explanation,
      knowledgePoint: m.knowledgePoint,
      mistakeCount: m.mistakeCount,
      lastMistakeTime: m.lastMistakeTime,
      mastered: m.mastered,
      createdTime: m.createdTime
    }))
  } catch (error) {
    console.error('获取错题列表失败:', error)
    ElMessage.error('加载错题失败')
  } finally {
    loading.value = false
  }
}

// 切换筛选模式
async function switchFilter(mode) {
  filterMode.value = mode
  await fetchMistakes()
}

// 获取筛选后的错题列表
const filteredMistakes = computed(() => {
  let result = mistakes.value

  // 应用搜索
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase().trim()
    result = result.filter(m =>
      m.question.toLowerCase().includes(keyword) ||
      m.knowledgePoint?.toLowerCase().includes(keyword)
    )
  }

  // 应用模式筛选
  result = result.filter(m => {
    if (filterMode.value === 'all') return true
    if (filterMode.value === 'unmastered') return !m.mastered
    if (filterMode.value === 'mastered') return m.mastered
    return false
  })

  return result
})

// 搜索错题
function handleSearch() {
  if (!searchKeyword.value.trim()) {
    return
  }
  isSearching.value = true
  // 搜索会自动触发 computed 重新计算
}

// 清空搜索
function clearSearch() {
  searchKeyword.value = ''
  isSearching.value = false
}

// 选择/取消选择错题
const toggleMistake = (mistake) => {
  if (selectedMistakes.value.has(mistake.id)) {
    selectedMistakes.value.delete(mistake.id)
  } else {
    selectedMistakes.value.add(mistake.id)
  }
}

// 是否全部选中
const isAllSelected = computed(() =>
  mistakes.value.length > 0 && selectedMistakes.value.size === mistakes.value.length
)

// 全选/反选
const toggleAll = () => {
  if (isAllSelected.value) {
    selectedMistakes.value.clear()
  } else {
    mistakes.value.forEach(m => selectedMistakes.value.add(m.id))
  }
}

// 标记为已掌握
const markAsMastered = async (mistake) => {
  try {
    await api.markMistakesMastered([mistake.questionId])
    mistake.mastered = true
    ElMessage.success('已标记为掌握')

    // 从列表中移除已掌握的错题
    if (filterMode.value !== 'all') {
      mistakes.value = mistakes.value.filter(m => m.id !== mistake.id)
    }
  } catch (error) {
    console.error('标记失败:', error)
    ElMessage.error('标记失败')
  }
}

// 刷新错题本
const refreshMistakes = () => {
  fetchMistakes()
}

// 点击卡片查看详情
const handleCardClick = (mistake) => {
  emit('select-mistake', mistake)
}

// 批量操作
const batchAction = async (action, actionName) => {
  const count = selectedMistakes.value.size
  if (count === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要${actionName}选中的 ${count} 个错题吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 获取选中错题的 questionId 列表
    const selectedMistakeList = mistakes.value.filter(m => selectedMistakes.value.has(m.id))
    const questionIds = selectedMistakeList.map(m => m.questionId)

    if (action === 'delete') {
      await api.deleteMistakes(questionIds)
    } else if (action === 'mastered') {
      await api.markMistakesMastered(questionIds)
    }

    selectedMistakes.value.clear()
    ElMessage.success(`${actionName}成功`)
    fetchMistakes()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error(`${actionName}失败`)
    }
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))

  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days} 天前`
  return date.toLocaleDateString('zh-CN')
}

// 获取题型标签
const getQuestionTypeLabel = (type) => {
  const labels = {
    'SINGLE_CHOICE': '单选',
    'MULTIPLE_CHOICE': '多选',
    'TRUE_FALSE': '判断',
    'FILL_BLANK': '填空',
    'CODE_SNIPPET': '代码'
  }
  return labels[type] || '未知'
}

// 获取题型标签类型
const getQuestionTypeTagType = (type) => {
  const types = {
    'single_choice': 'primary',
    'multiple_choice': 'success',
    'true_false': 'info',
    'fill_blank': 'warning',
    'code_snippet': 'danger'
  }
  return types[type] || ''
}

// 暴露方法给父组件
defineExpose({ refreshMistakes })
</script>

<template>
  <div class="mistake-list-pane" :class="{ 'is-embedded': embedded }">
    <!-- 筛选和搜索栏 -->
    <div class="filter-bar glass-panel">
      <div class="filter-group">
        <el-radio-group v-model="filterMode" @change="switchFilter(filterMode)" size="default" class="custom-radio-group">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="unmastered">未掌握</el-radio-button>
          <el-radio-button value="mastered">已掌握</el-radio-button>
        </el-radio-group>
      </div>

      <div class="search-group">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索题目或知识点..."
          :prefix-icon="Search"
          clearable
          @clear="clearSearch"
          @input="handleSearch"
          class="search-input apple-input"
        />
      </div>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-actions-bar" v-if="selectedMistakes.size > 0">
      <div class="selection-info">
        已选择 <strong>{{ selectedMistakes.size }}</strong> 个错题
      </div>
      <div class="batch-buttons">
        <el-button
          type="primary"
          :icon="FolderChecked"
          @click="batchAction('mastered', '标记为已掌握')"
          :disabled="loading"
          size="small"
        >
          标记为已掌握
        </el-button>
        <el-button
          type="danger"
          :icon="Delete"
          @click="batchAction('delete', '删除选中')"
          :disabled="loading"
          size="small"
        >
          删除
        </el-button>
      </div>
    </div>

    <!-- 错题列表 -->
    <div class="mistakes-list-wrapper">
      <CustomScroll>
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
          <el-icon class="is-loading" :size="32"><Loading /></el-icon>
          <p>加载中...</p>
        </div>

        <!-- 空状态 -->
        <el-empty
          v-else-if="!loading && filteredMistakes.length === 0"
          :description="searchKeyword.trim() ? '未找到相关错题' : '暂无错题记录'"
        >
          <template #image>
            <el-icon :size="80" color="#909399"><WarningFilled /></el-icon>
          </template>
        </el-empty>

        <!-- 错题列表 -->
        <div v-else class="mistakes-list">
          <!-- 全选框 -->
          <div class="select-all-bar" v-if="mistakes.length > 0">
            <el-checkbox
              :model-value="isAllSelected"
              @change="toggleAll"
            >
              全选
            </el-checkbox>
          </div>

          <!-- 错题卡片 -->
          <div
            v-for="mistake in filteredMistakes"
            :key="mistake.id"
            class="mistake-card"
            :class="{
              'is-selected': selectedMistakes.has(mistake.id),
              'is-mastered': mistake.mastered
            }"
            @click="handleCardClick(mistake)"
          >
            <div class="card-header">
              <div class="header-left">
                <el-checkbox
                  :model-value="selectedMistakes.has(mistake.id)"
                  @click.stop
                  @change="toggleMistake(mistake)"
                />
                <el-tag
                  :type="getQuestionTypeTagType(mistake.questionType)"
                  size="small"
                >
                  {{ getQuestionTypeLabel(mistake.questionType) }}
                </el-tag>
                <span class="mistake-count-badge">错误 {{ mistake.mistakeCount }} 次</span>
              </div>
              <div class="header-right">
                <el-tag v-if="mistake.mastered" type="success" size="small" effect="plain">
                  <el-icon><SuccessFilled /></el-icon>
                  已掌握
                </el-tag>
              </div>
            </div>

            <div class="card-content">
              <div class="question-text">
                {{ mistake.question }}
              </div>

              <div class="meta-info">
                <div class="knowledge-point" v-if="mistake.knowledgePoint">
                  <el-icon><PriceTag /></el-icon>
                  <span>{{ mistake.knowledgePoint }}</span>
                </div>
                <div class="time-info">
                  <el-icon><Clock /></el-icon>
                  <span>{{ formatTime(mistake.lastMistakeTime) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </CustomScroll>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/variables';

.mistake-list-pane {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 12px;
  box-sizing: border-box;
  padding: 16px;
}

/* ============ Glassmorphism 效果 ============ */
.glass-panel {
  background: rgba(255, 255, 255, 0.64);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.24);
  box-shadow: 0 2px 16px -1px rgba(0, 0, 0, 0.04);
  transition: all 0.25s cubic-bezier(0.25, 1, 0.5, 1);
}

html.dark .glass-panel {
  background: rgba(40, 40, 42, 0.64);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.mistake-list-pane.is-embedded {
  gap: 12px;
  padding: 0;
}

// 筛选和搜索栏
.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  flex-shrink: 0;
  border-radius: 16px;
}

.custom-radio-group {
  --el-fill-color-light: transparent;
  display: flex;
  gap: 3px;
  background: rgba(0, 0, 0, 0.04);
  padding: 3px;
  border-radius: 10px;
  flex-shrink: 0;

  html.dark & {
    background: rgba(255, 255, 255, 0.08);
  }

  :deep(.el-radio-button__inner) {
    border: none;
    background: transparent;
    border-radius: 8px;
    padding: 6px 12px;
    margin-right: 0;
    color: var(--el-text-color-regular);
    font-weight: 500;
    transition: all 0.2s cubic-bezier(0.25, 1, 0.5, 1);

    &:hover {
      color: var(--el-text-color-primary);
    }
  }

  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: rgba(255, 255, 255, 0.9);
    color: var(--el-text-color-primary);
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
    font-weight: 600;

    html.dark & {
      background: rgba(255, 255, 255, 0.15);
      color: #f5f5f7;
    }
  }

  :deep(.el-radio-button) {
    &:first-child .el-radio-button__inner {
      border-radius: 8px;
    }

    &:last-child .el-radio-button__inner {
      border-radius: 8px;
    }
  }
}

.search-group {
  flex: 1;
  max-width: 200px;

  .search-input {
    width: 100%;

    :deep(.el-input__wrapper) {
      background: rgba(255, 255, 255, 0.6);
      border: 1px solid rgba(0, 0, 0, 0.06);
      border-radius: 10px;
      padding: 4px 12px;
      transition: all 0.2s ease;
      box-shadow: none;

      &:hover {
        border-color: rgba(0, 0, 0, 0.12);
      }

      &.is-focus {
        border-color: var(--el-color-primary);
        background: rgba(255, 255, 255, 0.9);
      }

      html.dark & {
        background: rgba(255, 255, 255, 0.08);
        border-color: rgba(255, 255, 255, 0.1);

        &:hover {
          border-color: rgba(255, 255, 255, 0.15);
        }

        &.is-focus {
          background: rgba(255, 255, 255, 0.12);
          border-color: var(--el-color-primary);
        }
      }
    }
  }
}

// 批量操作栏
.batch-actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(64, 158, 255, 0.08);
  border-radius: 12px;
  border: 1px solid rgba(64, 158, 255, 0.15);
  animation: slideDown 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  flex-shrink: 0;

  html.dark & {
    background: rgba(64, 158, 255, 0.15);
    border-color: rgba(64, 158, 255, 0.25);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.selection-info {
  font-size: 13px;
  color: var(--el-text-color-regular);
  font-weight: 500;

  strong {
    color: var(--el-color-primary);
    font-size: 15px;
    margin: 0 2px;
  }
}

.batch-buttons {
  display: flex;
  gap: 10px;
}

// 错题列表容器
.mistakes-list-wrapper {
  flex: 1;
  min-height: 0;
  background: transparent;
  border-radius: 12px;
  overflow: hidden;
}

.mistakes-list {
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.select-all-bar {
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 10px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  border: 1px solid transparent;

  html.dark & {
    background: rgba(255, 255, 255, 0.03);
  }
}

// 错题卡片
.mistake-card {
  background: rgba(255, 255, 255, 0.64);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 16px;
  padding: 16px;
  overflow: hidden;
  transition: all 0.25s cubic-bezier(0.25, 1, 0.5, 1);
  cursor: pointer;
  position: relative;
  box-shadow: 0 2px 16px -1px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  gap: 12px;

  html.dark & {
    background: rgba(40, 40, 42, 0.64);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  &:hover {
    background: rgba(255, 255, 255, 0.8);
    box-shadow: 0 8px 32px -2px rgba(0, 0, 0, 0.08);
    transform: scale(1.01);

    html.dark & {
      background: rgba(50, 50, 52, 0.8);
    }
  }

  &:active {
    transform: scale(0.99);
  }

  &.is-selected {
    background: rgba(64, 158, 255, 0.12);
    border-color: rgba(64, 158, 255, 0.3);

    html.dark & {
      background: rgba(64, 158, 255, 0.2);
      border-color: rgba(64, 158, 255, 0.4);
    }
  }

  &.is-mastered {
    opacity: 0.6;
    filter: grayscale(0.8);

    &:hover {
      opacity: 0.9;
      filter: grayscale(0);
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mistake-count-badge {
  font-size: 10px;
  font-weight: 600;
  color: #ff453a;
  padding: 4px 8px;
  background: rgba(255, 69, 58, 0.12);
  border-radius: 6px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 6px;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-text {
  font-size: 15px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Text", "Segoe UI", Roboto, sans-serif;
  letter-spacing: -0.01em;

  html.dark & {
    color: #f5f5f7;
  }
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.knowledge-point,
.time-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s ease;

  .el-icon {
    font-size: 12px;
  }

  html.dark & {
    color: #86868b;
  }
}

.knowledge-point {
  background: rgba(0, 122, 255, 0.12);
  color: #007aff;

  html.dark & {
    background: rgba(0, 122, 255, 0.2);
    color: #0a84ff;
  }

  &:hover {
    background: rgba(0, 122, 255, 0.18);

    html.dark & {
      background: rgba(0, 122, 255, 0.3);
    }
  }
}

.time-info {
  background: rgba(0, 0, 0, 0.04);

  html.dark & {
    background: rgba(255, 255, 255, 0.08);
  }

  &:hover {
    background: rgba(0, 0, 0, 0.06);

    html.dark & {
      background: rgba(255, 255, 255, 0.12);
    }
  }
}

// 加载状态
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 16px;
  color: #86868b;

  html.dark & {
    color: #86868b;
  }

  .el-icon {
    color: var(--el-color-primary);
  }
}

// 响应式设计
@media (max-width: 1024px) {
  .search-group {
    max-width: 160px;
  }
}

@media (max-width: 768px) {
  .mistake-list-pane {
    padding: 12px;
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;

    .search-group {
      max-width: none;
    }
  }

  .batch-actions-bar {
    flex-direction: column;
    gap: 10px;
    align-items: stretch;

    .batch-buttons {
      justify-content: center;
    }
  }

  .mistakes-list {
    padding: 4px;
  }
}
</style>
